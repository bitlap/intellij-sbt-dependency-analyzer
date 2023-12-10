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
  private val log = Logger.getInstance(getClass)

  protected final def getCommandOutputLines(project: Project, command: String): List[String] =
    val shellCommunication       = SbtShellCommunication.forProject(project)
    val executed: Future[String] = shellCommunication.command(command)
    val res                      = Await.result(executed, Constants.TIMEOUT)
    val result                   = res.split(Constants.LINE_SEPARATOR).toList.filter(_.startsWith("[info]"))
    if (result.isEmpty) {
      log.warn("Sbt Dependency Analyzer cannot find any output lines")
    }
    // see https://github.com/JetBrains/intellij-scala/blob/idea232.x/sbt/sbt-impl/src/org/jetbrains/sbt/shell/communication.scala
    // 1 second between multiple commands
    waitInterval()

    result
  end getCommandOutputLines

  def executeCommand(project: Project): T

end SbtShellOutputAnalysisTask

object SbtShellOutputAnalysisTask:

  final case class LibraryModuleID(
    organization: String,
    name: String,
    revision: String,
    configurations: Option[String] = None, // not used
    isChanging: Boolean = false,           // not used
    isTransitive: Boolean = false,         // not used
    isForce: Boolean = false               // not used
  )

  // moduleName
  final val shellOutputResultRegex   = "(\\[info\\])(\\s|\\t)*(.*)".r
  final val moduleNameInputRegex     = "(\\[info\\])(\\s|\\t)*(.*)(\\s|\\t)*/(\\s|\\t)*moduleName".r
  final val rootModuleNameInputRegex = "(\\[info\\])(\\s|\\t)*moduleName".r

  // libraryDependencies
  final val libraryDependenciesInputRegex     = "(\\[info\\])(\\s|\\t)*(.*)(\\s|\\t)*/(\\s|\\t)*libraryDependencies".r
  final val rootLibraryDependenciesInputRegex = "(\\[info\\])(\\s|\\t)*libraryDependencies".r
  final val libraryDependenciesOutputRegex    = "List\\((.*)\\)".r
  final val shellOutputStarResultRegex        = "(\\[info\\])(\\s|\\t)*(\\*)(\\s|\\t)*(.*)".r
  // libraryDependencies ModuleID
  final val libraryDependenciesOutput1 = "(.*):(.*):(.*)".r
  final val libraryDependenciesOutput2 = "(.*):(.*):(.*):(.*)".r
  final val libraryDependenciesOutput3 = "(.*):(.*):(.*):(.*);(.*)".r
  final val libraryDependenciesOutput4 = "(.*):(.*):(.*):(.*)(\\s|\\t)*(.*)".r

  lazy val sbtModuleNamesTask: SbtShellOutputAnalysisTask[Map[String, String]] = new ModuleNameTask

  lazy val organizationTask: SbtShellOutputAnalysisTask[String] = new OrganizationTask

  lazy val reloadTask: SbtShellOutputAnalysisTask[Unit] = new ReloadTask

  lazy val refreshSnapshotsTask: SbtShellOutputAnalysisTask[Unit] = new RefreshSnapshotsTask

  lazy val libraryDependenciesTask: SbtShellOutputAnalysisTask[Map[String, List[LibraryModuleID]]] =
    new LibraryDependenciesTask

end SbtShellOutputAnalysisTask
