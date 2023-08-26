package bitlap.sbt.analyzer.task

import com.intellij.openapi.project.Project

/** Process the `sbt reload` command, load new setting for sbt shell.
 *
 *  @author
 *    梦境迷离
 *  @version 1.0,2023/8/19
 */
final class ReloadTask extends SbtShellOutputAnalysisTask[Unit]:

  override def executeCommand(project: Project): Unit = getCommandOutputLines(project, "reload")

end ReloadTask
