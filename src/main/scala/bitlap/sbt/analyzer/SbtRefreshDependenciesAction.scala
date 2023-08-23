package bitlap.sbt.analyzer

import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.sbt.project.SbtProjectSystem

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.action.RefreshAllExternalProjectsAction
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/23
 */
final class SbtRefreshDependenciesAction extends RefreshAllExternalProjectsAction {

  getTemplatePresentation.setText(SbtPluginBundle.message("sbt.dependency.analyzer.refresh.dependencies.text"))

  getTemplatePresentation.setDescription(
    SbtPluginBundle.message("sbt.dependency.analyzer.refresh.dependencies.description")
  )

  override def beforeActionPerformedUpdate(e: AnActionEvent): Unit = {
    SbtDependencyAnalyzerContributor.isValid.set(false)
    super.beforeActionPerformedUpdate(e)
  }
}
