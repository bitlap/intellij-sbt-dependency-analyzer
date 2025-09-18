package bitlap.sbt.analyzer.task

import com.intellij.openapi.project.Project

/** Handles the `sbt reload` command to load new settings and refresh the environment for the sbt shell.
 */
final class ReloadTask extends SbtShellOutputAnalysisTask[Unit]:

  override def executeCommand(project: Project): Unit = getCommandOutputLines(project, "reload")

end ReloadTask
