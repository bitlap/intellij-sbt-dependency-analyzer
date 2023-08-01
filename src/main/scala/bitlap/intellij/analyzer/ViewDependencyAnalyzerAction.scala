package bitlap.intellij.analyzer

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.externalSystem.dependency.analyzer.*
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.model.project.*
import com.intellij.openapi.externalSystem.model.project.dependencies.ArtifactDependencyNode
import com.intellij.openapi.externalSystem.model.project.dependencies.DependencyScopeNode
import com.intellij.openapi.externalSystem.model.project.dependencies.ProjectDependencyNode
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.view.ExternalSystemNode
import com.intellij.openapi.externalSystem.view.ModuleNode
import com.intellij.openapi.externalSystem.view.ProjectNode
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.module.Module

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/1
 */
final class ViewDependencyAnalyzerAction        extends AbstractDependencyAnalyzerAction[ExternalSystemNode[?]] {}
final class ProjectViewDependencyAnalyzerAction extends AbstractDependencyAnalyzerAction[Module]                {}
final class ToolbarDependencyAnalyzerAction extends DependencyAnalyzerAction() {}
