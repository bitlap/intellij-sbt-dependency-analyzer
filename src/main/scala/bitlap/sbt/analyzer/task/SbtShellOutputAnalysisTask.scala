package bitlap
package sbt
package analyzer
package task

import scala.concurrent.*

import org.jetbrains.sbt.shell.SbtShellCommunication

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

import Constants.*

/** Tasks depend on the output of the SBT console.
 */
trait SbtShellOutputAnalysisTask[T]:
  private val LOG = Logger.getInstance(getClass)

  protected final def getCommandOutputLines(project: Project, command: String): List[String] =
    val shellCommunication       = SbtShellCommunication.forProject(project)
    val executed: Future[String] = shellCommunication.command(command)
    val res                      = Await.result(executed, Constants.TIMEOUT)
    val result                   = res.split(Constants.LINE_SEPARATOR).toList.filter(_.startsWith("[info]"))
    if (result.isEmpty) {
      LOG.warn("Sbt Dependency Analyzer cannot find any output lines")
    }
    // see https://github.com/JetBrains/intellij-scala/blob/idea232.x/sbt/sbt-impl/src/org/jetbrains/sbt/shell/communication.scala
    // 1 second between multiple commands
    waitInterval()

    result
  end getCommandOutputLines

  def executeCommand(project: Project): T

end SbtShellOutputAnalysisTask

object SbtShellOutputAnalysisTask:

  // moduleName
  final val SHELL_OUTPUT_RESULT_REGEX    = "(\\[info\\])(\\s|\\t)*(.*)".r
  final val MODULE_NAME_INPUT_REGEX      = "(\\[info\\])(\\s|\\t)*(.*)(\\s|\\t)*/(\\s|\\t)*moduleName".r
  final val ROOT_MODULE_NAME_INPUT_REGEX = "(\\[info\\])(\\s|\\t)*moduleName".r

  lazy val sbtModuleNamesTask: SbtShellOutputAnalysisTask[Map[String, String]] = new ModuleNameTask

  lazy val organizationTask: SbtShellOutputAnalysisTask[String] = new OrganizationTask

  lazy val reloadTask: SbtShellOutputAnalysisTask[Unit] = new ReloadTask

  lazy val refreshSnapshotsTask: SbtShellOutputAnalysisTask[Unit] = new RefreshSnapshotsTask

end SbtShellOutputAnalysisTask
