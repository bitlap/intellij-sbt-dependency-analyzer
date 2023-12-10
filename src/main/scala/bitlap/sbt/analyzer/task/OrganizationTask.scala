package bitlap.sbt.analyzer.task

import com.intellij.openapi.project.Project

/** Process the `sbt organization` command, get current project organization as artifact's groupId.
 */
final class OrganizationTask extends SbtShellOutputAnalysisTask[String]:

  import SbtShellOutputAnalysisTask.*

  override def executeCommand(project: Project): String =
    val outputLines = getCommandOutputLines(project, "organization")
    outputLines.lastOption.getOrElse("") match
      case shellOutputResultRegex(_, _, org) =>
        org.trim
      case _ => null

  end executeCommand

end OrganizationTask
