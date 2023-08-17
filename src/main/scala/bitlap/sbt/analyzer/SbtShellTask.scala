package bitlap.sbt.analyzer

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.*

import bitlap.sbt.analyzer.Constants.*

import org.jetbrains.sbt.shell.SbtShellCommunication

import com.intellij.openapi.project.Project

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/11
 */
trait SbtShellTask[T] {

  final def getCommandOutputLines(project: Project, command: String): List[String] = {
    val comms       = SbtShellCommunication.forProject(project)
    val outputLines = ListBuffer[String]()
    val executed = comms.command(
      command,
      new StringBuilder(),
      SbtShellCommunication.listenerAggregator {
        case SbtShellCommunication.Output(line) =>
          outputLines.append(line.trim)
        case _ =>
      }
    )
    Await.result(executed, 5.minutes)
    outputLines.toList.filter(_.startsWith("[info]"))
  }

  def executeTask(project: Project): T

}

object SbtShellTask {

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

  lazy val sbtModuleNamesTask: SbtShellTask[Map[String, String]] = new SbtShellTask[Map[String, String]]:

    override def executeTask(project: Project): Map[String, String] = {
      val mms                      = getCommandOutputLines(project, "moduleName")
      val moduleIdSbtModuleNameMap = scala.collection.mutable.HashMap[String, String]()
      if (mms.size % 2 == 0) {
        for (i <- 0 until mms.size - 1 by 2) {
          moduleIdSbtModuleNameMap.put(mms(i).trim, mms(i + 1).trim)
        }
      } else if (mms.size == 1) moduleIdSbtModuleNameMap.put(SingleSbtModule, mms(0))

      moduleIdSbtModuleNameMap.map { (k, v) =>
        val key = k match
          case moduleNameInputRegex(_, _, moduleName, _, _) => moduleName.trim
          case rootModuleNameInputRegex(_, _)               => RootSbtModule
          case SingleSbtModule                              => SingleSbtModule
          case _                                            => Constants.Empty_String

        val value = v match
          case shellOutputResultRegex(_, _, sbtModuleName) => sbtModuleName.trim
          case _                                           => Constants.Empty_String

        key -> value

      }.filter(kv => kv._1 != Constants.Empty_String && kv._2 != Constants.Empty_String).toMap
    }

  lazy val organizationTask: SbtShellTask[String] = new SbtShellTask[String]:

    override def executeTask(project: Project): String = {
      val outputLines = getCommandOutputLines(project, "organization")
      outputLines.lastOption.getOrElse("") match
        case shellOutputResultRegex(_, _, org) =>
          org.trim
        case _ => null
    }

  lazy val libraryDependenciesTask: SbtShellTask[Map[String, List[LibraryModuleID]]] = new SbtShellTask[Map[String, List[LibraryModuleID]]]:

    private def moduleIDArtifact(output: String): Option[LibraryModuleID] = {
      output.trim match
        case libraryDependenciesOutput4(group, artifact, revision, _, _) =>
          Some(LibraryModuleID(group.trim, artifact.trim, revision.trim))
        case libraryDependenciesOutput3(group, artifact, revision, conf, _, _) =>
          Some(LibraryModuleID(group.trim, artifact.trim, revision.trim))  
        case libraryDependenciesOutput2(group, artifact, revision, conf) =>
          Some(LibraryModuleID(group.trim, artifact.trim, revision.trim))  
        case libraryDependenciesOutput1(group, artifact, revision) =>
          Some(LibraryModuleID(group.trim, artifact.trim, revision.trim))  
        case _ =>
          None
    }

    override def executeTask(project: Project): Map[String, List[LibraryModuleID]] = {
      val outputLines              = getCommandOutputLines(project, "libraryDependencies")
      val moduleIdSbtLibrariesMap = scala.collection.mutable.HashMap[String, String]()
      // single module
      if (outputLines.startsWith("[info] *")) {
        val libraries = outputLines.map {
          case shellOutputStarResultRegex(_, _, _, _, artifact) =>
            moduleIDArtifact(artifact)
          case _ => None
        }.collect { case Some(value) =>
          value
        }
        return Map(SingleSbtModule -> libraries)

      }
      // multi-module
      if (outputLines.size % 2 == 0) {
        for (i <- 0 until outputLines.size - 1 by 2) {
          moduleIdSbtLibrariesMap.put(outputLines(i).trim, outputLines(i + 1).trim)
        }
      }

      moduleIdSbtLibrariesMap.map { (k, v) =>
        val key = k match
          case libraryDependenciesInputRegex(_, _, moduleName, _, _) => moduleName.trim
          case rootLibraryDependenciesInputRegex(_, _)               => RootSbtModule
          case _                                                     => Constants.Empty_String

        val value = v match
          case shellOutputResultRegex(_, _, output) =>
            output.trim match
              case libraryDependenciesOutputRegex(modules) =>
                modules.split(",").toList.map(_.trim).map(moduleIDArtifact).collect { case Some(value) =>
                  value
                }
              case _ => List.empty
          case _ => List.empty

        key -> value
      }.collect {
        case (k, v) if k != Constants.Empty_String => k -> v
      }.toMap

    }
}
