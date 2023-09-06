package bitlap
package sbt
package analyzer
package action

import org.jetbrains.sbt.project.SbtProjectSystem

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/23
 */
final class SbtRefreshDependenciesAction extends BaseRefreshDependenciesAction:

  override lazy val eventText: String = SbtDependencyAnalyzerBundle.message("analyzer.refresh.dependencies.text")

  override lazy val eventDescription: String =
    SbtDependencyAnalyzerBundle.message("analyzer.refresh.dependencies.description")

  override def actionPerformed(e: AnActionEvent): Unit = {
    SbtDependencyAnalyzerContributor.isAvailable.set(false)
    ExternalSystemUtil.refreshProjects(
      new ImportSpecBuilder(e.getProject, SbtProjectSystem.Id).dontReportRefreshErrors().build()
    )
  }

end SbtRefreshDependenciesAction
