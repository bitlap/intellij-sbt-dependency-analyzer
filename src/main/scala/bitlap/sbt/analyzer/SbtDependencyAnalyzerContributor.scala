package bitlap.sbt.analyzer

import java.util
import java.util.Collections
import java.util.concurrent.{ ConcurrentHashMap, Executors }
import java.util.concurrent.atomic.AtomicLong

import scala.collection.mutable.ListBuffer
import scala.concurrent.*
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

import bitlap.sbt.analyzer.model.ModuleContext
import bitlap.sbt.analyzer.parser.*

import org.jetbrains.plugins.scala.project.ModuleExt
import org.jetbrains.sbt.language.utils.SbtDependencyUtils
import org.jetbrains.sbt.project.SbtProjectSystem
import org.jetbrains.sbt.project.data.ModuleNode
import org.jetbrains.sbt.shell.SbtShellCommunication

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
import com.intellij.openapi.util.text.StringUtil

import kotlin.jvm.functions

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/1
 */
final class SbtDependencyAnalyzerContributor(project: Project) extends DependencyAnalyzerContributor {

  import SbtDependencyAnalyzerContributor.*

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

  private var organization: String = null
  private val orgRegex             = "(\\[.*\\])(\\s|\\t)(.*)".r

  private def getOrganization(project: Project): String = {
    if (organization != null) return organization
    val comms       = SbtShellCommunication.forProject(project)
    val outputLines = ListBuffer[String]()
    val executed = comms.command(
      "organization",
      new StringBuilder(),
      SbtShellCommunication.listenerAggregator {
        case SbtShellCommunication.Output(line) =>
          outputLines.append(line)
        case _ =>
      }
    )
    Await.result(executed, 5.minutes)
    outputLines.filter(_.startsWith("[info]")).lastOption.getOrElse("") match
      case orgRegex(level, space, org) =>
        organization = org.trim
      case _ =>
    organization
  }

  private def getOrRefreshData(moduleData: ModuleData): util.List[DependencyScopeNode] = {
    // FIXME
    val org             = getOrganization(project)
    val allaModulePaths = projects.values().asScala.map(d => d.getModuleName -> d.getLinkedExternalProjectPath).toMap
    if (moduleData.getModuleName == "project") return Collections.emptyList()
    configurationNodesMap.computeIfAbsent(
      moduleData.getLinkedExternalProjectPath,
      _ => moduleData.loadDependencies(project, org, allaModulePaths)
    )
  }
}

object SbtDependencyAnalyzerContributor {
  private val id                           = new AtomicLong(0)
  private def scope(name: String): DAScope = DAScope(name, StringUtil.toTitleCase(name))
  private final val DefaultConfiguration   = scope("default")

  final val Module_Data = Key.create[ModuleData]("SbtDependencyAnalyzerContributor.ModuleData")

  private def scopedKey(project: String, scope: DependencyScopeEnum, cmd: String): String = {
    if (project == null || project.isEmpty) s"$scope / $cmd"
    else s"$project / $scope / $cmd"
  }

  private def fileName(scope: DependencyScopeEnum, parserTypeEnum: ParserTypeEnum): String = {
    parserTypeEnum match
      case ParserTypeEnum.DOT =>
        s"/target/dependencies-${scope.toString.toLowerCase}.${parserTypeEnum.suffix}"
  }

  private def rootNode(dependencyScope: DependencyScopeEnum, project: Project): DependencyScopeNode = {
    val scopeDisplayName = "project " + project.getBasePath + " (" + dependencyScope.toString + ")"
    val node = new DependencyScopeNode(
      id.getAndIncrement(),
      dependencyScope.toString,
      scopeDisplayName,
      dependencyScope.toString
    )
    node.setResolutionState(ResolutionState.RESOLVED)
    node
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
    def toScope: DAScope = scope(dependencyScopeNode.getScope)
  }

  given ExecutionContext =
    ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(2 * Runtime.getRuntime.availableProcessors()))

  extension (moduleData: ModuleData) {

    def loadDependencies(
      project: Project,
      org: String,
      allaModulePaths: Map[String, String]
    ): util.List[DependencyScopeNode] = {
      val module = findModule(project, moduleData)
      val comms  = SbtShellCommunication.forProject(project)
      // if module is itself a build module, skip build module
      val buildModule = SbtDependencyUtils.getBuildModule(module)
      if (buildModule.isEmpty) return Collections.emptyList()
      val promiseList    = ListBuffer[Promise[DependencyScopeNode]]()
      val moduleId       = moduleData.getId.split(" ")(0)
      val moduleName     = moduleData.getModuleName
      val declaredFuture = Future { DependencyUtil.getUnifiedCoordinates(module, project) }
      val res = for {
        declared <- declaredFuture
        result <- Future {
          DependencyScopeEnum.values.toList.foreach { scope =>
            val promise = Promise[DependencyScopeNode]()
            promiseList.append(promise)
            comms.command(
              scopedKey(moduleId, scope, ParserTypeEnum.DOT.cmd),
              new StringBuilder(),
              SbtShellCommunication.listenerAggregator {
                case SbtShellCommunication.TaskStart =>
                case SbtShellCommunication.TaskComplete =>
                  val root = DependencyParserFactory
                    .getInstance(ParserTypeEnum.DOT)
                    .buildDependencyTree(
                      ModuleContext(
                        moduleData.getLinkedExternalProjectPath + fileName(scope, ParserTypeEnum.DOT),
                        moduleName,
                        scope,
                        DependencyUtil.scalaMajorVersion(module),
                        org,
                        allaModulePaths,
                        module.isScalaJs,
                        module.isScalaNative
                      ),
                      rootNode(scope, project),
                      declared
                    )
                  promise.success(root)
                case SbtShellCommunication.ErrorWaitForInput =>
                  promise.failure(new Exception(SbtPluginBundle.message("sbt.dependency.analyzer.error.unknown")))
                case SbtShellCommunication.Output(line) =>
                  if (line.startsWith(s"[error]") && !promise.isCompleted) {
                    promise.failure(new Exception(SbtPluginBundle.message("sbt.dependency.analyzer.error")))
                  }

              }
            )
          }
          Future.sequence(promiseList.toList.map(_.future))
        }.flatten
      } yield result

      Await.result(res.map(_.asJava), 10.minutes)
    }
  }
}
