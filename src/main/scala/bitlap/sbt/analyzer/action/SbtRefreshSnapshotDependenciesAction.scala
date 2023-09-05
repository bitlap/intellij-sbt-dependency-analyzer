package bitlap.sbt.analyzer.action

import scala.annotation.nowarn
import scala.jdk.CollectionConverters.*

import bitlap.sbt.analyzer.*
import bitlap.sbt.analyzer.task.*

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.statistics.ExternalSystemActionsCollector
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.fileEditor.FileDocumentManager

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/23
 */
final class SbtRefreshSnapshotDependenciesAction extends BaseRefreshDependenciesAction:

  override lazy val eventText: String =
    SbtDependencyAnalyzerBundle.message("analyzer.refresh.snapshot.dependencies.text")

  override lazy val eventDescription: String =
    SbtDependencyAnalyzerBundle.message("analyzer.refresh.snapshot.dependencies.description")

  @nowarn("cat=deprecation")
  override def actionPerformed(e: AnActionEvent): Unit = {
    SbtDependencyAnalyzerContributor.isValid.set(false)
    SbtShellOutputAnalysisTask.refreshSnapshotsTask.executeCommand(e.getProject)
    // no need to trigger refresh project, but for update view
     super.actionPerformed(e)
  }

end SbtRefreshSnapshotDependenciesAction
