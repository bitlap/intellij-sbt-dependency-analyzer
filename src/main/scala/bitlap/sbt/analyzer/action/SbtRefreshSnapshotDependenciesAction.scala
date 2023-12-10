package bitlap.sbt.analyzer.action

import bitlap.sbt.analyzer.*
import bitlap.sbt.analyzer.task.*
import bitlap.sbt.analyzer.util.SbtUtils

import org.jetbrains.sbt.project.SbtProjectSystem

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil

final class SbtRefreshSnapshotDependenciesAction extends BaseRefreshDependenciesAction:

  override lazy val eventText: String =
    SbtDependencyAnalyzerBundle.message("analyzer.refresh.snapshot.dependencies.text")

  override lazy val eventDescription: String =
    SbtDependencyAnalyzerBundle.message("analyzer.refresh.snapshot.dependencies.description")

  override def actionPerformed(e: AnActionEvent): Unit = {
    SbtDependencyAnalyzerContributor.isAvailable.set(false)
    SbtShellOutputAnalysisTask.refreshSnapshotsTask.executeCommand(e.getProject)
    SbtUtils.refreshProject(e.getProject)
  }

end SbtRefreshSnapshotDependenciesAction
