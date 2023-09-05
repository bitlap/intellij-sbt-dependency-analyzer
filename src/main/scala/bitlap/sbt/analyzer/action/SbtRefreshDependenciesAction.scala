package bitlap
package sbt
package analyzer
package action

import com.intellij.openapi.actionSystem.AnActionEvent

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/23
 */
final class SbtRefreshDependenciesAction extends BaseRefreshDependenciesAction:

  override lazy val eventText: String = SbtDependencyAnalyzerBundle.message("analyzer.refresh.dependencies.text")

  override lazy val eventDescription: String =
    SbtDependencyAnalyzerBundle.message("analyzer.refresh.dependencies.description")

  override def actionPerformed(e: AnActionEvent): Unit = {
    SbtDependencyAnalyzerContributor.isValid.set(false)
    super.actionPerformed(e)
  }

end SbtRefreshDependenciesAction
