package bitlap.sbt.analyzer

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util
import java.util.Collections
import java.util.concurrent.{ ConcurrentHashMap, Executors }
import java.util.concurrent.atomic.AtomicLong

import scala.collection.mutable.ListBuffer
import scala.concurrent.*
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

import bitlap.sbt.analyzer.DependencyUtil.*
import bitlap.sbt.analyzer.model.ModuleContext
import bitlap.sbt.analyzer.parser.*
import bitlap.sbt.analyzer.parser.ParserTypeEnum
import bitlap.sbt.analyzer.task.*

import org.jetbrains.plugins.scala.project.ModuleExt
import org.jetbrains.sbt.project.SbtProjectSystem
import org.jetbrains.sbt.project.data.ModuleNode

import com.intellij.buildsystem.model.unified.UnifiedCoordinates
import com.intellij.openapi.Disposable
import com.intellij.openapi.externalSystem.dependency.analyzer.{ DependencyAnalyzerDependency as Dependency, * }
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerDependency.Data
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.dependencies.*
import com.intellij.openapi.externalSystem.model.task.*
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.{ ExternalSystemApiUtil, ExternalSystemBundle }
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.*

import kotlin.jvm.functions

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/1
 */
final class SbtDependencyAnalyzerContributor(project: Project) extends DependencyAnalyzerContributor {

  import SbtDependencyAnalyzerContributor.*

  private var organization: String                           = _
  private var sbtModules: Map[String, String]                = Map.empty
  private var declaredDependencies: List[UnifiedCoordinates] = List.empty

  private lazy val projects: ConcurrentHashMap[DependencyAnalyzerProject, ModuleNode] =
    ConcurrentHashMap[DependencyAnalyzerProject, ModuleNode]()
  private lazy val dependencyMap: ConcurrentHashMap[Long, Dependency] = ConcurrentHashMap[Long, Dependency]()

  private lazy val configurationNodesMap: ConcurrentHashMap[String, util.List[DependencyScopeNode]] =
    ConcurrentHashMap[String, util.List[DependencyScopeNode]]()

  override def getDependencies(
    externalProject: DependencyAnalyzerProject
  ): util.List[Dependency] = {
    val moduleData = projects.get(externalProject)
    if (moduleData == null) return Collections.emptyList()
    val scopeNodes = getOrRefreshData(moduleData)
    getDependencies(moduleData, scopeNodes)
  }

  override def getDependencyScopes(
    externalProject: DependencyAnalyzerProject
  ): util.List[Dependency.Scope] = {
    val moduleData = projects.get(externalProject)
    if (moduleData == null) return Collections.emptyList()
    getOrRefreshData(moduleData).asScala.map(_.toScope).asJava
    ////    DependencyScope.values.toList.map(d => scope(d.toString.toLowerCase)).toList.asJava
  }

  override def getProjects: util.List[DependencyAnalyzerProject] = {
    if (projects.isEmpty) {
      val projectDataManager = ProjectDataManager.getInstance()
      projectDataManager.getExternalProjectsData(project, SbtProjectSystem.Id).asScala.foreach { projectInfo =>
        if (projectInfo.getExternalProjectStructure != null) {
          val projectStructure = projectInfo.getExternalProjectStructure
          ExternalSystemApiUtil.findAll(projectStructure, ProjectKeys.MODULE).asScala.foreach { moduleNode =>
            val moduleData = moduleNode.getData
            val module     = findModule(project, moduleData)
            if (module != null) {
              val externalProject = DAProject(module, moduleData.getModuleName)
              if (!DependencyUtil.ignoreModuleAnalysis(module)) {
                projects.put(externalProject, new ModuleNode(moduleData))
              }
            }
          }

        }

      }

    }
    projects.keys.asScala.toList.sortBy(_.getModule.getName).asJava

  }

  override def whenDataChanged(listener: functions.Function0[kotlin.Unit], parentDisposable: Disposable): Unit = {
    val progressManager = ExternalSystemProgressNotificationManager.getInstance()
    progressManager.addNotificationListener(
      new ExternalSystemTaskNotificationListenerAdapter() {
        override def onEnd(id: ExternalSystemTaskId): Unit = {
          if (id.getType != ExternalSystemTaskType.RESOLVE_PROJECT) ()
          else if (id.getProjectSystemId != SbtProjectSystem.Id) ()
          else {
            // if dependencies have changed, we must delete all analysis files (.dot)
            // however, this can only be used to monitor whether the view is open
            projects
              .values()
              .asScala
              .map(d => d.getLinkedExternalProjectPath)
              .foreach(SbtDependencyAnalyzerContributor.deleteExistAnalysisFiles)

            projects.clear()
            configurationNodesMap.clear()
            dependencyMap.clear()
            listener.invoke()
          }
        }
      },
      parentDisposable
    )
  }

  private def getDependencies(
    moduleData: ModuleData,
    scopeNodes: util.List[DependencyScopeNode]
  ): util.List[Dependency] = {
    if (scopeNodes.isEmpty) return Collections.emptyList()
    val dependencies = ListBuffer[Dependency]()
    val root         = DAModule(moduleData.getModuleName)
    root.putUserData(Module_Data, moduleData)

    val rootDependency = DADependency(root, DefaultConfiguration, null, Collections.emptyList())
    dependencies.append(rootDependency)
    for (scopeNode <- scopeNodes.asScala) {
      val scope = scopeNode.toScope
      for (dependencyNode <- scopeNode.getDependencies.asScala) {
        addDependencies(rootDependency, scope, dependencyNode, dependencies, moduleData.getLinkedExternalProjectPath)
      }
    }
    dependencies.asJava
  }

  private def createDependency(
    dependencyNode: DependencyNode,
    scope: Dependency.Scope,
    usage: Dependency
  ): Dependency = {
    dependencyNode match
      case rn: ReferenceNode =>
        val dependency = dependencyMap.get(dependencyNode.getId)
        if (dependency == null) null
        else {
          DADependency(dependency.getData, scope, usage, dependency.getStatus)
        }
      case _ =>
        val dependencyData = dependencyNode.getDependencyData(projects)
        if (dependencyData == null) null
        else {
          val status = dependencyNode.getStatus(usage, dependencyData)
          val dep    = DADependency(dependencyData, scope, usage, status)
          dependencyMap.put(dependencyNode.getId, dep)
          dep
        }
  }

  private def addDependencies(
    usage: Dependency,
    scope: Dependency.Scope,
    dependencyNode: DependencyNode,
    dependencies: ListBuffer[Dependency],
    projectDir: String
  ): Unit = {
    val dependency = createDependency(dependencyNode, scope, usage)
    if (dependency == null) {} else {
      dependencies.append(dependency)
      for (node <- dependencyNode.getDependencies.asScala) {
        addDependencies(dependency, scope, node, dependencies, projectDir)
      }
    }

  }

  private def getOrganization(project: Project): String = {
    if (organization != null) return organization
    organization = SbtShellOutputAnalysisTask.organizationTask.executeCommand(project)
    organization
  }

  private def getSbtModules(project: Project): Map[String, String] = {
    if (sbtModules.nonEmpty) return sbtModules
    sbtModules = SbtShellOutputAnalysisTask.sbtModuleNamesTask.executeCommand(project)
    sbtModules
  }

  private def getDeclaredDependencies(project: Project, moduleData: ModuleData): List[UnifiedCoordinates] = {
    if (declaredDependencies.nonEmpty) return declaredDependencies
    val module = findModule(project, moduleData)
    declaredDependencies = DependencyUtil.getUnifiedCoordinates(module)
    declaredDependencies
  }

  private def getOrRefreshData(moduleData: ModuleData): util.List[DependencyScopeNode] = {
    // use to link dependencies between modules.
    // obtain the mapping of module name to file path.
    val moduleNamePaths = () =>
      projects.values().asScala.map(d => d.getModuleName -> d.getLinkedExternalProjectPath).toMap
    val org        = () => getOrganization(project)
    val declared   = () => getDeclaredDependencies(project, moduleData)
    val sbtModules = () => getSbtModules(project)
    if (moduleData.getModuleName == Constants.Project) return Collections.emptyList()

    configurationNodesMap.computeIfAbsent(
      moduleData.getLinkedExternalProjectPath,
      _ => moduleData.loadDependencies(project, org(), moduleNamePaths(), sbtModules(), declared())
    )
  }
}

object SbtDependencyAnalyzerContributor {

  def validFile(file: String): Boolean = {
    val lifespan     = 1000 * 60 * 60L
    val lastModified = Path.of(file).toFile.lastModified()
    System.currentTimeMillis() <= lastModified + lifespan
  }

  def deleteExistAnalysisFiles(modulePath: String): Unit = {
    DependencyScopeEnum.values
      .map(scope => Path.of(modulePath + analysisFilePath(scope, ParserTypeEnum.DOT)))
      .foreach(p => Files.deleteIfExists(p))
  }

  // ===========================================extensions==============================================================
  extension (projectDependencyNode: ProjectDependencyNode) {

    def getModuleData(projects: ConcurrentHashMap[DependencyAnalyzerProject, ModuleNode]): ModuleData = {
      projects.values.asScala
        .map(_.data)
        .find(_.getLinkedExternalProjectPath == projectDependencyNode.getProjectPath)
        .orNull
    }
  }

  extension (node: DependencyNode) {

    def getDependencyData(projects: ConcurrentHashMap[DependencyAnalyzerProject, ModuleNode]): Dependency.Data = {
      node match {
        case pdn: ProjectDependencyNode =>
          val data       = DAModule(pdn.getProjectName)
          val moduleData = pdn.getModuleData(projects)
          data.putUserData(Module_Data, moduleData)
          data
        case adn: ArtifactDependencyNode =>
          DAArtifact(adn.getGroup, adn.getModule, adn.getVersion)
        case _ => null
      }
    }

    def getStatus(usage: Dependency, data: Dependency.Data): util.List[Dependency.Status] = {
      val status = ListBuffer[Dependency.Status]()
      if (node.getResolutionState == ResolutionState.UNRESOLVED) {
        val message = ExternalSystemBundle.message("external.system.dependency.analyzer.warning.unresolved")
        status.append(DAWarning(message))
      }
      val selectionReason = node.getSelectionReason
      data match
        case dataArtifact: Data.Artifact if selectionReason == "Evicted By" =>
          status.append(DAOmitted.INSTANCE)
          val conflictedVersion = usage.getData match
            case artifact: Data.Artifact =>
              if (artifact.getArtifactId == dataArtifact.getArtifactId) {
                artifact.getVersion
              } else null
            case _ => null

          if (conflictedVersion != null) {
            val message = ExternalSystemBundle.message(
              "external.system.dependency.analyzer.warning.version.conflict",
              conflictedVersion
            )
            status.append(DAWarning(message))
          }
        case _ =>
      status.asJava
    }
  }

  extension (dependencyScopeNode: DependencyScopeNode) {
    def toScope: DAScope = toDAScope(dependencyScopeNode.getScope)
  }

  given ExecutionContext =
    ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(2 * Runtime.getRuntime.availableProcessors()))

  extension (moduleData: ModuleData) {

    def loadDependencies(
      project: Project,
      organization: String,
      moduleNamePaths: Map[String, String],
      sbtModules: Map[String, String],
      declared: List[UnifiedCoordinates]
    ): util.List[DependencyScopeNode] = {
      val module = findModule(project, moduleData)
      if (DependencyUtil.ignoreModuleAnalysis(module)) return Collections.emptyList()

      // if the analysis files already exist (.dot), use it directly.
      def executeCommandOrReadExistsFile(
        parserTypeEnum: ParserTypeEnum,
        scope: DependencyScopeEnum
      ): Future[DependencyScopeNode] = {
        val moduleId   = moduleData.getId.split(" ")(0)
        val moduleName = moduleData.getModuleName
        val file       = moduleData.getLinkedExternalProjectPath + analysisFilePath(scope, ParserTypeEnum.DOT)
        // File cache for one hour
        if (Files.exists(Path.of(file)) && validFile(file)) {
          Future {
            DependencyParserFactory
              .getInstance(parserTypeEnum)
              .buildDependencyTree(
                ModuleContext(
                  file,
                  moduleName,
                  scope,
                  scalaMajorVersion(module),
                  organization,
                  moduleNamePaths,
                  module.isScalaJs,
                  module.isScalaNative,
                  if (sbtModules.isEmpty) Map(moduleId -> module.getName)
                  else sbtModules
                ),
                rootNode(scope, project),
                declared
              )
          }
        } else {
          SbtShellDependencyAnalysisTask.dependencyDotTask.executeCommand(
            project,
            moduleData,
            scope,
            organization,
            moduleNamePaths,
            sbtModules,
            declared
          )
        }

      }

      try {
        Await.result(
          Future
            .sequence(DependencyScopeEnum.values.toList.map(executeCommandOrReadExistsFile(ParserTypeEnum.DOT, _)))
            .map(_.asJava),
          10.minutes
        )
      } catch {
        case e: Throwable => throw e
      }
    }

  }
}
