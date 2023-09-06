package bitlap.sbt.analyzer.action

import bitlap.sbt.analyzer.*
import bitlap.sbt.analyzer.task.*

import org.jetbrains.sbt.project.SbtProjectSystem

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/23
 */
final class SbtRefreshSnapshotDependenciesAction extends BaseRefreshDependenciesAction:

  override lazy val eventText: String =
    SbtDependencyAnalyzerBundle.message("analyzer.refresh.snapshot.dependencies.text")

  override lazy val eventDescription: String =
    SbtDependencyAnalyzerBundle.message("analyzer.refresh.snapshot.dependencies.description")

  override def actionPerformed(e: AnActionEvent): Unit = {
    SbtDependencyAnalyzerContributor.isAvailable.set(false)
    SbtShellOutputAnalysisTask.refreshSnapshotsTask.executeCommand(e.getProject)
    ExternalSystemUtil.refreshProjects(
      new ImportSpecBuilder(e.getProject, SbtProjectSystem.Id).dontReportRefreshErrors().build()
    )
  }

end SbtRefreshSnapshotDependenciesAction
