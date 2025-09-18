package bitlap.sbt.analyzer.task

import com.intellij.openapi.project.Project

/** Handles the `sbt organization` command to retrieve the current project's organization, which typically serves as the
 *  groupId for artifacts in dependency management.
 */
final class OrganizationTask extends SbtShellOutputAnalysisTask[String]:

  import SbtShellOutputAnalysisTask.*

  override def executeCommand(project: Project): String =
    val outputLines = getCommandOutputLines(project, "organization")
    outputLines.lastOption.getOrElse("") match
      case SHELL_OUTPUT_RESULT_REGEX(_, _, org) =>
        org.trim
      case _ => null

  end executeCommand

end OrganizationTask
