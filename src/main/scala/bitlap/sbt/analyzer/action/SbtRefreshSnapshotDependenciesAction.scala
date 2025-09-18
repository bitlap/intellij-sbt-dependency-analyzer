package bitlap.sbt.analyzer.action

import bitlap.sbt.analyzer.*
import bitlap.sbt.analyzer.task.*
import bitlap.sbt.analyzer.util.SbtReimportProject

import com.intellij.openapi.actionSystem.AnActionEvent

final class SbtRefreshSnapshotDependenciesAction extends BaseRefreshDependenciesAction:

  override lazy val eventText: String =
    SbtDependencyAnalyzerBundle.message("analyzer.refresh.snapshot.dependencies.text")

  override lazy val eventDescription: String =
    SbtDependencyAnalyzerBundle.message("analyzer.refresh.snapshot.dependencies.description")

  override def actionPerformed(e: AnActionEvent): Unit = {
    SbtShellOutputAnalysisTask.refreshSnapshotsTask.executeCommand(e.getProject)
    SbtReimportProject.ReimportProjectPublisher.onReimportProject(e.getProject)
  }

end SbtRefreshSnapshotDependenciesAction
