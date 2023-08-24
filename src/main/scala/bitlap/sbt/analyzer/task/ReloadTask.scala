package bitlap.sbt.analyzer.task

import bitlap.sbt.analyzer.Constants

import com.intellij.openapi.project.Project

/** Process the `sbt reload` command, get new setting for sbt shell.
 *
 *  @author
 *    梦境迷离
 *  @version 1.0,2023/8/19
 */
final class ReloadTask extends SbtShellOutputAnalysisTask[Unit] {

  import SbtShellOutputAnalysisTask.*

  override def executeCommand(project: Project): Unit = {
    getCommandOutputLines(project, "reload")
  }

}
