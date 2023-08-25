package bitlap
package sbt
package analyzer
package task

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ Await, Promise }
import scala.concurrent.duration.*
import scala.util.{ Failure, Success }

import bitlap.sbt.analyzer.*
import bitlap.sbt.analyzer.Constants.*

import org.jetbrains.sbt.shell.SbtShellCommunication

import com.intellij.openapi.project.Project

/** Tasks depend on the output of the SBT console.
 *
 *  @author
 *    梦境迷离
 *  @version 1.0,2023/8/11
 */
trait SbtShellOutputAnalysisTask[T] {

  protected final def getCommandOutputLines(project: Project, command: String): List[String] = {
    val comms       = SbtShellCommunication.forProject(project)
    val outputLines = ListBuffer[String]()
    val promise     = Promise[List[String]]
    val executed = comms.command(
      command,
      new StringBuilder(),
      SbtShellCommunication.listenerAggregator {
        case SbtShellCommunication.Output(line) =>
          outputLines.append(line.trim)
        case SbtShellCommunication.TaskComplete =>
          promise.success(outputLines.result())
        case _ =>
      }
    )
    Await.result(executed.flatMap(_ => promise.future.map(_.filter(_.startsWith("[info]")))), Constants.timeout)
  }

  def executeCommand(project: Project): T

}

object SbtShellOutputAnalysisTask {

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

  lazy val libraryDependenciesTask: SbtShellOutputAnalysisTask[Map[String, List[LibraryModuleID]]] =
    new LibraryDependenciesTask
}
