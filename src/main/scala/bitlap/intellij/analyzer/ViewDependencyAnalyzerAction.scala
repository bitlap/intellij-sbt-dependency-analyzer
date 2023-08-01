package bitlap.intellij.analyzer

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
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
import com.intellij.openapi.util.text.StringUtil

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/1
 */
final class ViewDependencyAnalyzerAction extends AbstractDependencyAnalyzerAction[ExternalSystemNode[?]] {
  override def getDependencyScope(anActionEvent: AnActionEvent, data: ExternalSystemNode[_]): String = ???

  override def getModule(anActionEvent: AnActionEvent, data: ExternalSystemNode[_]): Module = ???

  override def getSelectedData(anActionEvent: AnActionEvent): ExternalSystemNode[_] = ???

  override def getSystemId(anActionEvent: AnActionEvent): ProjectSystemId = ???

  override def getDependencyData(
    anActionEvent: AnActionEvent,
    data: ExternalSystemNode[_]
  ): DependencyAnalyzerDependency.Data = ???
}

final class ProjectViewDependencyAnalyzerAction extends AbstractDependencyAnalyzerAction[Module] {
  override def getDependencyScope(anActionEvent: AnActionEvent, data: Module): String = ???

  override def getModule(anActionEvent: AnActionEvent, data: Module): Module = ???

  override def getSelectedData(anActionEvent: AnActionEvent): Module = ???

  override def getSystemId(anActionEvent: AnActionEvent): ProjectSystemId = ???

  override def getDependencyData(anActionEvent: AnActionEvent, data: Module): DependencyAnalyzerDependency.Data = ???
}

final class ToolbarDependencyAnalyzerAction extends DependencyAnalyzerAction() {
  override def getSystemId(anActionEvent: AnActionEvent): ProjectSystemId = ???

  override def isEnabledAndVisible(anActionEvent: AnActionEvent): Boolean = ???

  override def setSelectedState(dependencyAnalyzerView: DependencyAnalyzerView, anActionEvent: AnActionEvent): Unit =
    ???
}
