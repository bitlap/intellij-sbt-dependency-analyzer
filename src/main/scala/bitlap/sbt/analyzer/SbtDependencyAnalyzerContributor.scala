package bitlap.sbt.analyzer

import java.nio.file.*
import java.util
import java.util.{ Collections, List as JList }
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

import scala.collection.mutable.ListBuffer
import scala.concurrent.*
import scala.jdk.CollectionConverters.*

import bitlap.sbt.analyzer.DependencyUtils.*
import bitlap.sbt.analyzer.component.*
import bitlap.sbt.analyzer.model.*
import bitlap.sbt.analyzer.parser.*
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
import com.intellij.openapi.externalSystem.util.*
import com.intellij.openapi.project.Project

import kotlin.jvm.functions

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/1
 */
final class SbtDependencyAnalyzerContributor(project: Project) extends DependencyAnalyzerContributor {

  import SbtDependencyAnalyzerContributor.*

  @volatile
  private var organization: String = _

  @volatile
  private var ideaModuleIdSbtModules: Map[String, String] = Map.empty

  @volatile
  private var declaredDependencies: List[UnifiedCoordinates] = List.empty

  private lazy val projects: ConcurrentHashMap[DependencyAnalyzerProject, ModuleNode] =
    ConcurrentHashMap[DependencyAnalyzerProject, ModuleNode]()
  private lazy val dependencyMap: ConcurrentHashMap[Long, Dependency] = ConcurrentHashMap[Long, Dependency]()

  private lazy val configurationNodesMap: ConcurrentHashMap[String, JList[DependencyScopeNode]] =
    ConcurrentHashMap[String, JList[DependencyScopeNode]]()

  override def getDependencies(
    externalProject: DependencyAnalyzerProject
  ): JList[Dependency] = {
    val moduleData = projects.get(externalProject)
    if (moduleData == null) return Collections.emptyList()
    val scopeNodes = getOrRefreshData(moduleData)
    getDependencies(moduleData, scopeNodes)
  }

  override def getDependencyScopes(
    externalProject: DependencyAnalyzerProject
  ): JList[Dependency.Scope] = {
    val moduleData = projects.get(externalProject)
    if (moduleData == null) return Collections.emptyList()
    getOrRefreshData(moduleData).asScala.map(_.toScope).asJava
    ////    DependencyScope.values.toList.map(d => scope(d.toString.toLowerCase)).toList.asJava
  }

  override def getProjects: JList[DependencyAnalyzerProject] = {
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
              if (!DependencyUtils.ignoreModuleAnalysis(module)) {
                projects.put(externalProject, new ModuleNode(moduleData))
              }
            }
          }

        }

      }

    }

    // root -> top
    // this will result in the plugin always executing the root/dependencyDot when attempting to open the view for the first time,
    // and generating dot files for all modules by default.
    // TODO: Problem: Problem: It will be slow when used for the first time, Exclude root module?
    val (root, others) = projects.asScala.partition(_._2.getLinkedExternalProjectPath == project.getBasePath)
    (root.keys.toList ++ others.keys.toList.sortBy(_.getModule.getName)).asJava

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
              .foreach(deleteExistAnalysisFiles)

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

  private def deleteExistAnalysisFiles(modulePath: String)(using ParserTypeEnum): Unit = {
    DependencyScopeEnum.values
      .map(scope => Path.of(modulePath + analysisFilePath(scope, summon[ParserTypeEnum])))
      .foreach(p => Files.deleteIfExists(p))
  }

  private def getDependencies(
    moduleData: ModuleData,
    scopeNodes: JList[DependencyScopeNode]
  ): JList[Dependency] = {
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

  private def getOrganization(project: Project): String =
    // When force a refresh, we must re-read the settings, such organization,moduleName
    if (organization != null && SbtDependencyAnalyzerContributor.isValid.get()) return organization
    organization = SbtShellOutputAnalysisTask.organizationTask.executeCommand(project)
    organization

  private def getIdeaModuleIdSbtModules(project: Project): Map[String, String] =
    if (ideaModuleIdSbtModules.nonEmpty && SbtDependencyAnalyzerContributor.isValid.get()) return ideaModuleIdSbtModules
    ideaModuleIdSbtModules = SbtShellOutputAnalysisTask.sbtModuleNamesTask.executeCommand(project)
    ideaModuleIdSbtModules

  private def getDeclaredDependencies(project: Project, moduleData: ModuleData): List[UnifiedCoordinates] =
    if (declaredDependencies.nonEmpty && SbtDependencyAnalyzerContributor.isValid.get()) return declaredDependencies
    val module = findModule(project, moduleData)
    declaredDependencies = DependencyUtils.getUnifiedCoordinates(module)
    declaredDependencies

  private def getOrRefreshData(moduleData: ModuleData)(using ParserTypeEnum): JList[DependencyScopeNode] = {
    // use to link dependencies between modules.
    // obtain the mapping of module name to file path.
    if (moduleData.getModuleName == Constants.Project) return Collections.emptyList()

    val result = configurationNodesMap.computeIfAbsent(
      moduleData.getLinkedExternalProjectPath,
      _ =>
        moduleData.loadDependencies(
          project,
          getOrganization(project),
          projects.values().asScala.map(d => d.getModuleName -> d.getLinkedExternalProjectPath).toMap,
          getIdeaModuleIdSbtModules(project),
          getDeclaredDependencies(project, moduleData)
        )
    )
    Option(result).getOrElse(Collections.emptyList())
  }
}

object SbtDependencyAnalyzerContributor:

  final val isValid             = new AtomicBoolean(true)
  private final val isNotifying = new AtomicBoolean(false)

  private def isValidFile(file: String): Boolean = {
    if (isValid.get()) {
      val lastModified = Path.of(file).toFile.lastModified()
      System.currentTimeMillis() <= lastModified + Constants.fileLifespan
    } else {
      isValid.getAndSet(true)
    }
  }

  // ===========================================extensions==============================================================
  extension (projectDependencyNode: ProjectDependencyNode)

    def getModuleData(projects: ConcurrentHashMap[DependencyAnalyzerProject, ModuleNode]): ModuleData =
      projects.values.asScala
        .map(_.data)
        .find(_.getLinkedExternalProjectPath == projectDependencyNode.getProjectPath)
        .orNull
    end getModuleData

  end extension

  extension (node: DependencyNode)

    def getDependencyData(projects: ConcurrentHashMap[DependencyAnalyzerProject, ModuleNode]): Dependency.Data =
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
    end getDependencyData

    def getStatus(usage: Dependency, data: Dependency.Data): JList[Dependency.Status] =
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

    end getStatus

  end extension

  extension (dependencyScopeNode: DependencyScopeNode) def toScope: DAScope = toDAScope(dependencyScopeNode.getScope)
  end extension

  extension (moduleData: ModuleData)

    def loadDependencies(
      project: Project,
      organization: String,
      ideaModuleNamePaths: Map[String, String],
      ideaModuleIdSbtModules: Map[String, String],
      declared: List[UnifiedCoordinates]
    )(using ParserTypeEnum): JList[DependencyScopeNode] =
      val module = findModule(project, moduleData)
      if (DependencyUtils.ignoreModuleAnalysis(module)) return Collections.emptyList()

      // if the analysis files already exist (.dot), use it directly.
      def executeCommandOrReadExistsFile(
        scope: DependencyScopeEnum
      ): DependencyScopeNode =
        val moduleId = moduleData.getId.split(" ")(0)
        val file     = moduleData.getLinkedExternalProjectPath + analysisFilePath(scope, summon[ParserTypeEnum])
        // File cache for one hour
        if (!isNotifying.get() && Files.exists(Path.of(file)) && isValidFile(file)) {
          DependencyParserFactory
            .getInstance(summon[ParserTypeEnum])
            .buildDependencyTree(
              ModuleContext(
                file,
                moduleId,
                scope,
                scalaMajorVersion(module),
                organization,
                ideaModuleNamePaths,
                module.isScalaJs,
                module.isScalaNative,
                if (ideaModuleIdSbtModules.isEmpty) Map(moduleId -> module.getName)
                else ideaModuleIdSbtModules
              ),
              createRootScopeNode(scope, project),
              declared
            )
        } else {
          SbtShellDependencyAnalysisTask.dependencyDotTask.executeCommand(
            project,
            moduleData,
            scope,
            organization,
            ideaModuleNamePaths,
            ideaModuleIdSbtModules,
            declared
          )
        }
      end executeCommandOrReadExistsFile

      try {

        if (isNotifying.get()) {
          // must reload project to enable it
          SbtShellOutputAnalysisTask.reloadTask.executeCommand(project)
        }

        val result =
          DependencyScopeEnum.values.toList.map(executeCommandOrReadExistsFile)

        isNotifying.set(false)
        result.asJava

      } catch {
        case e: Throwable =>
          e match
            case _: AnalyzerCommandNotFoundException =>
              if (isNotifying.compareAndSet(false, true)) {
                SbtDependencyAnalyzerNotifier.notifyAndAddDependencyTreePlugin(project)
              }
            case ue: AnalyzerCommandUnknownException =>
              SbtDependencyAnalyzerNotifier.notifyUnknownError(project, ue.command, ue.moduleId, ue.scope)
            case _ =>
          null
      }
    end loadDependencies
  end extension

end SbtDependencyAnalyzerContributor
