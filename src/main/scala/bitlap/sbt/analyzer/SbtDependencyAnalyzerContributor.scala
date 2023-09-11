package bitlap.sbt.analyzer

import java.nio.file.*
import java.util
import java.util.{ Collections, List as JList }
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

import scala.collection.mutable.ListBuffer
import scala.concurrent.*
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

import bitlap.sbt.analyzer.activity.*
import bitlap.sbt.analyzer.model.*
import bitlap.sbt.analyzer.parser.*
import bitlap.sbt.analyzer.task.*
import bitlap.sbt.analyzer.util.{ DependencyUtils, Notifications, SbtUtils }
import bitlap.sbt.analyzer.util.DependencyUtils.*

import org.jetbrains.plugins.scala.project.ModuleExt
import org.jetbrains.sbt.project.SbtProjectSystem
import org.jetbrains.sbt.project.data.ModuleNode

import com.intellij.buildsystem.model.unified.UnifiedCoordinates
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
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
              if (!DependencyUtils.canIgnoreModule(module)) {
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

  private def deleteExistAnalysisFiles(modulePath: String): Unit = {
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
    // When force refresh, we will not re-read the settings, such organization,moduleName, because refreshing makes efficiency lower.
    // Usually, Uses do not change frequently, so it's better to keep caching until the view is reopen.
    val org = SettingsState.getSettings(project).organization
    if (org != null && org != Constants.EmptyString) {
      return org
    }

    if (organization != null) return organization
    organization = SbtShellOutputAnalysisTask.organizationTask.executeCommand(project)
    organization

  private def getIdeaModuleIdSbtModules(project: Project): Map[String, String] =
    if (ideaModuleIdSbtModules.nonEmpty) return ideaModuleIdSbtModules
    ideaModuleIdSbtModules = SbtShellOutputAnalysisTask.sbtModuleNamesTask.executeCommand(project)
    ideaModuleIdSbtModules

  private def getDeclaredDependencies(project: Project, moduleData: ModuleData): List[UnifiedCoordinates] =
    if (declaredDependencies.nonEmpty) return declaredDependencies
    val module = findModule(project, moduleData)
    declaredDependencies = DependencyUtils.getDeclaredDependency(module).map(_.getCoordinates)
    declaredDependencies

  private def getOrRefreshData(moduleData: ModuleData): JList[DependencyScopeNode] = {
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

object SbtDependencyAnalyzerContributor extends SettingsState.SettingsChangeListener:

  final val isAvailable = new AtomicBoolean(true)

  // if data change
  override def onAnalyzerConfigurationChanged(project: Project, settingsState: SettingsState): Unit = {
    // TODO
    isAvailable.set(false)
    SbtUtils.refreshProject(project)
  }

  ApplicationManager.getApplication.getMessageBus.connect().subscribe(SettingsState._Topic, this)

  private final val isNotifying = new AtomicBoolean(false)

  private def isValidFile(project: Project, file: String): Boolean = {
    if (isAvailable.get()) {
      val lastModified = Path.of(file).toFile.lastModified()
      System.currentTimeMillis() <= lastModified + SettingsState.getSettings(project).fileCacheTimeout * 1000
    } else {
      isAvailable.getAndSet(true)
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
    ): JList[DependencyScopeNode] =
      val module   = findModule(project, moduleData)
      val moduleId = moduleData.getId.split(" ")(0)

      if (DependencyUtils.canIgnoreModule(module)) return Collections.emptyList()

      if (isNotifying.get() && SbtUtils.untilProjectReady(project)) {
        // must reload project to enable it
        SbtShellOutputAnalysisTask.reloadTask.executeCommand(project)
        isNotifying.compareAndSet(true, false)
      }

      // if the analysis files already exist (.dot), use it directly.
      def executeCommandOrReadExistsFile(
        scope: DependencyScopeEnum
      ): DependencyScopeNode =
        val file     = moduleData.getLinkedExternalProjectPath + analysisFilePath(scope, summon[ParserTypeEnum])
        val useCache = !isNotifying.get() && Files.exists(Path.of(file)) && isValidFile(project, file)
        // File cache for one hour
        if (useCache) {
          DependencyParserFactory
            .getInstance(summon[ParserTypeEnum])
            .buildDependencyTree(
              ModuleContext(
                file,
                moduleId,
                scope,
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

      val result = ListBuffer[DependencyScopeNode]()
      import scala.util.control.Breaks.*
      // break, no more commands will be executed
      breakable {
        val settings = SettingsState.getSettings(project)
        for (scope <- DependencyScopeEnum.values) {
          var node: DependencyScopeNode = null
          try {

            if (settings.disableAnalyzeProvided && scope == DependencyScopeEnum.Provided) {} else if (
              settings.disableAnalyzeTest && scope == DependencyScopeEnum.Test
            ) {} else if (settings.disableAnalyzeCompile && scope == DependencyScopeEnum.Compile) {} else {
              node = executeCommandOrReadExistsFile(scope)
            }
            if (node != null) {
              result.append(node)
            }
          } catch {
            case _: AnalyzerCommandNotFoundException =>
              if (isNotifying.compareAndSet(false, true)) {
                Notifications.notifyAndCreateSdapFile(project)
              }
              break()
            case ue: AnalyzerCommandUnknownException =>
              Notifications.notifyUnknownError(project, ue.command, ue.moduleId, ue.scope)
              break()
            case e =>
              throw e
          }
        }
      }

      result.toList.asJava
    end loadDependencies
  end extension

end SbtDependencyAnalyzerContributor
