package bitlap.sbt.analyzer

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.action.RefreshAllExternalProjectsAction

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/23
 */
final class SbtRefreshDependenciesAction extends RefreshAllExternalProjectsAction {

  getTemplatePresentation.setText(
    SbtDependencyAnalyzerBundle.message("sbt.dependency.analyzer.refresh.dependencies.text")
  )

  getTemplatePresentation.setDescription(
    SbtDependencyAnalyzerBundle.message("sbt.dependency.analyzer.refresh.dependencies.description")
  )

  override def beforeActionPerformedUpdate(e: AnActionEvent): Unit = {
    SbtDependencyAnalyzerContributor.isValid.set(false)
    super.beforeActionPerformedUpdate(e)
  }
}
