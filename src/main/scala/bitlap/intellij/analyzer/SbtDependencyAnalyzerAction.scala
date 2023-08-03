package bitlap.intellij.analyzer

import scala.jdk.CollectionConverters.*

import org.jetbrains.sbt.project.SbtProjectSystem

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.externalSystem.dependency.analyzer.*
import com.intellij.openapi.externalSystem.model.*
import com.intellij.openapi.externalSystem.model.project.*
import com.intellij.openapi.externalSystem.model.project.dependencies.ArtifactDependencyNode
import com.intellij.openapi.externalSystem.model.project.dependencies.DependencyScopeNode
import com.intellij.openapi.externalSystem.model.project.dependencies.ProjectDependencyNode
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.view.ExternalSystemNode
import com.intellij.openapi.externalSystem.view.ModuleNode
import com.intellij.openapi.externalSystem.view.ProjectNode
import com.intellij.openapi.module.Module

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/1
 */
final class ViewDependencyAnalyzerAction extends AbstractDependencyAnalyzerAction[ExternalSystemNode[?]] {

  override def getDependencyScope(anActionEvent: AnActionEvent, selectedData: ExternalSystemNode[_]): String = {
    val node = selectedData.findDependencyNode(classOf[DependencyScopeNode])
    if (node == null) return null
    node.getScope
  }

  override def getModule(anActionEvent: AnActionEvent, selectedData: ExternalSystemNode[_]): Module = {
    val project = anActionEvent.getProject
    if (project == null) return null
    val node = selectedData.findNode(classOf[ModuleNode])
    if (node == null) return null
    val data = node.getData
    if (data != null) return findModule(project, data)

    val projectNode = selectedData.findNode(classOf[ProjectNode])
    if (projectNode == null) return null
    val projectData = projectNode.getData
    if (projectNode == null) return null
    findModule(project, projectData)
  }

  override def getSelectedData(anActionEvent: AnActionEvent): ExternalSystemNode[_] =
    anActionEvent.getData(ExternalSystemDataKeys.SELECTED_NODES).asScala.headOption.orNull

  override def getSystemId(anActionEvent: AnActionEvent): ProjectSystemId = SbtProjectSystem.Id

  override def getDependencyData(
    anActionEvent: AnActionEvent,
    selectedData: ExternalSystemNode[_]
  ): DependencyAnalyzerDependency.Data = {
    selectedData.getData match
      case pd: ProjectData => DAModule(pd.getInternalName)
      case md: ModuleData  => DAModule(md.getModuleName)
      case _ =>
        selectedData.getDependencyNode match
          case pdn: ProjectDependencyNode  => DAModule(pdn.getProjectName)
          case adn: ArtifactDependencyNode => DAArtifact(adn.getGroup, adn.getModule, adn.getVersion)
  }
}

final class ProjectViewDependencyAnalyzerAction extends AbstractDependencyAnalyzerAction[Module] {
  override def getDependencyScope(anActionEvent: AnActionEvent, data: Module): String = null

  override def getModule(anActionEvent: AnActionEvent, selectedData: Module): Module = selectedData

  override def getSelectedData(anActionEvent: AnActionEvent): Module = {
    val module = anActionEvent.getData(PlatformCoreDataKeys.MODULE)
    if (module == null) return null
    if (ExternalSystemApiUtil.isExternalSystemAwareModule(SbtProjectSystem.Id, module)) {
      module
    } else null

  }

  override def getSystemId(anActionEvent: AnActionEvent): ProjectSystemId = SbtProjectSystem.Id

  override def getDependencyData(
    anActionEvent: AnActionEvent,
    selectedData: Module
  ): DependencyAnalyzerDependency.Data = DAModule(selectedData.getName)
}

final class ToolbarDependencyAnalyzerAction extends DependencyAnalyzerAction() {
  private val viewAction = ViewDependencyAnalyzerAction()

  override def getSystemId(anActionEvent: AnActionEvent): ProjectSystemId = SbtProjectSystem.Id

  override def isEnabledAndVisible(anActionEvent: AnActionEvent): Boolean = true

  override def setSelectedState(dependencyAnalyzerView: DependencyAnalyzerView, anActionEvent: AnActionEvent): Unit =
    viewAction.setSelectedState(dependencyAnalyzerView, anActionEvent)
}
