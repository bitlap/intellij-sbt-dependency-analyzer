package bitlap.sbt.analyzer

import java.util
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ Promise, * }
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

import bitlap.sbt.analyzer.parser.*

import org.jetbrains.plugins.scala.packagesearch.SbtDependencyModifier
import org.jetbrains.sbt.language.utils.SbtDependencyUtils
import org.jetbrains.sbt.project.SbtProjectSystem
import org.jetbrains.sbt.project.data.ModuleNode
import org.jetbrains.sbt.shell.SbtShellCommunication
import org.jetbrains.sbt.shell.action.SbtNodeAction

import com.intellij.buildsystem.model.DeclaredDependency
import com.intellij.externalSystem.ExternalDependencyModificator
import com.intellij.openapi.Disposable
import com.intellij.openapi.externalSystem.dependency.analyzer.{ DependencyAnalyzerDependency as Dependency, * }
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.dependencies.*
import com.intellij.openapi.externalSystem.model.task.*
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.{ ExternalSystemApiUtil, ExternalSystemBundle }
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.*
import com.intellij.openapi.util.text.StringUtil

import kotlin.jvm.functions

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/1
 */
final class SbtDependencyAnalyzerContributor(project: Project) extends DependencyAnalyzerContributor {

  import SbtDependencyAnalyzerContributor.*

  private lazy val projects              = ConcurrentHashMap[DependencyAnalyzerProject, ModuleNode]()
  private lazy val configurationNodesMap = ConcurrentHashMap[String, util.List[DependencyScopeNode]]()
  private lazy val dependencyMap         = ConcurrentHashMap[Long, Dependency]()

  override def getDependencies(
    externalProject: DependencyAnalyzerProject
  ): util.List[Dependency] = {
    val moduleData = projects.get(externalProject)
    if (moduleData == null) Collections.emptyList()
    val scopeNodes = getOrRefreshData(moduleData)
    getDependencies(moduleData, scopeNodes)
  }

  override def getDependencyScopes(
    externalProject: DependencyAnalyzerProject
  ): util.List[Dependency.Scope] = {
    val moduleData = projects.get(externalProject)
    if (moduleData == null) Collections.emptyList()
    getOrRefreshData(moduleData).asScala.map(_.toScope).asJava
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
              projects.put(externalProject, new ModuleNode(moduleData))
            }
          }

        }

      }

    }
    projects.keys.asScala.toList.asJava

  }

  override def whenDataChanged(listener: functions.Function0[kotlin.Unit], parentDisposable: Disposable): Unit = {
    val progressManager = ExternalSystemProgressNotificationManager.getInstance()
    progressManager.addNotificationListener(
      new ExternalSystemTaskNotificationListenerAdapter() {
        override def onEnd(id: ExternalSystemTaskId): Unit = {
          if (id.getType != ExternalSystemTaskType.RESOLVE_PROJECT) ()
          else if (id.getProjectSystemId != SbtProjectSystem.Id) ()
          else {
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
    scopeNodes.asScala.view
      .map(sn => sn.toScope -> sn.getDependencies.asScala)
      .foreach((scope, dependencyList) =>
        dependencyList.foreach { dependencyNode =>
          addDependencies(rootDependency, scope, dependencyNode, dependencies, moduleData.getLinkedExternalProjectPath)
        }
      )
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
          val status = dependencyNode.getStatus(dependencyData)
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
    if (dependency != null) {
      dependencies.append(dependency)
      for (node <- dependencyNode.getDependencies.asScala) {
        addDependencies(dependency, scope, node, dependencies, projectDir)
      }
    }

  }

  private def getOrRefreshData(moduleData: ModuleData): util.List[DependencyScopeNode] = {
    configurationNodesMap.computeIfAbsent(
      moduleData.getLinkedExternalProjectPath,
      _ => moduleData.loadDependencies(project)
    )
  }
}

object SbtDependencyAnalyzerContributor {

  private def scope(name: String): DAScope = DAScope(name, StringUtil.toTitleCase(name))

  final val DefaultConfiguration = scope("default")

  final val Module_Data = Key.create[ModuleData]("SbtDependencyAnalyzerContributor.ModuleData")

  extension (projectDependencyNode: ProjectDependencyNode) {

    def getModuleData(projects: ConcurrentHashMap[DependencyAnalyzerProject, ModuleNode]): ModuleData = {
      projects.values.asScala.map(_.data).find(_.getId == projectDependencyNode.getProjectPath).orNull
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

    def getStatus(data: Dependency.Data): util.List[Dependency.Status] = {
      val status = ListBuffer[Dependency.Status]()
      if (node.getResolutionState == ResolutionState.UNRESOLVED) {
        val message = ExternalSystemBundle.message("external.system.dependency.analyzer.warning.unresolved")
        status.append(DAWarning(message))
      }
      val selectionReason = node.getSelectionReason
      if (
        data.isInstanceOf[Dependency.Data.Artifact] && selectionReason != null && selectionReason.startsWith(
          "between versions"
        )
      ) {
        val idx               = selectionReason.indexOf("and ")
        val conflictedVersion = selectionReason.substring(idx + 4)
        if (conflictedVersion.nonEmpty) {
          val message = ExternalSystemBundle.message(
            "external.system.dependency.analyzer.warning.version.conflict",
            conflictedVersion
          )
          status.append(DAWarning(message))
        }
      }
      status.asJava
    }
  }

  extension (dependencyScopeNode: DependencyScopeNode) {
    def toScope: DAScope = scope(dependencyScopeNode.getScope)
  }

  private def scopedKey(project: String, scope: DependencyScope, cmd: String): String = {
    if (project == null || project.isEmpty) s"$scope / $cmd"
    else s"$project / $scope / $cmd"
  }

  private def fileName(scope: DependencyScope): String = {
    s"/target/dependencies-${scope.toString.toLowerCase}.dot"
  }

  private def rootNode(dependencyScope: DependencyScope): DependencyScopeNode = {
    val node = new DependencyScopeNode(
      id.getAndIncrement(),
      dependencyScope.toString,
      dependencyScope.toString,
      dependencyScope.toString
    )
    node.setResolutionState(ResolutionState.RESOLVED)
    node
  }

  extension (moduleData: ModuleData) {

    def loadDependencies(project: Project): util.List[DependencyScopeNode] = {
      val module = findModule(project, moduleData)
      val comms  = SbtShellCommunication.forProject(project)
      if (moduleData.getModuleName.endsWith("-build")) return Collections.emptyList()
      val promiseList = ListBuffer[Promise[DependencyScopeNode]]()
      DependencyScope.values.toList.foreach { scope =>
        val promise = Promise[DependencyScopeNode]()
        promiseList.append(promise)
        comms.command(
          scopedKey(module.getName, scope, "dependencyDot"),
          new StringBuilder(),
          SbtShellCommunication.listenerAggregator {
            case SbtShellCommunication.TaskStart =>
            case SbtShellCommunication.TaskComplete =>
              val root = rootNode(scope)
              root.getDependencies.addAll(
                DependencyGraphBuilderFactory
                  .getInstance(GraphBuilderEnum.Dot)
                  .buildDependencyTree(moduleData.getLinkedExternalProjectPath + fileName(scope))
              )
              promise.success(root)
            case SbtShellCommunication.ErrorWaitForInput =>
              promise.failure(new Exception(SbtPluginBundle.message("sbt.dependency.analyzer.error")))
            case SbtShellCommunication.Output(line) =>
          }
        )
      }
      import concurrent.ExecutionContext.Implicits.global
      val result = Future.sequence(promiseList.toList.map(_.future))
      Await.result(result.map(_.asJava), 30.minutes)
    }
  }
}
