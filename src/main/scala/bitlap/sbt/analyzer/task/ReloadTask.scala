package bitlap.sbt.analyzer.task

import com.intellij.openapi.project.Project

/** Process the `sbt reload` command, load new setting for sbt shell.
 */
final class ReloadTask extends SbtShellOutputAnalysisTask[Unit]:

  override def executeCommand(project: Project): Unit = getCommandOutputLines(project, "reload")

end ReloadTask
