package bitlap.sbt.analyzer

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.*

import org.jetbrains.sbt.shell.SbtShellCommunication

import com.intellij.openapi.project.Project

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/11
 */
trait SbtShellTask[T] {

  def executeTask(project: Project): T

}

object SbtShellTask {

  final val shellOutputResultRegex = "(\\[info\\])(\\s|\\t)*(.*)".r
  final val cmdNameRegex           = "(\\[info\\])(\\s|\\t)*(.*)(\\s|\\t)*/(\\s|\\t)*moduleName".r
  final val SingleSbtModule        = "$SingleModule$"

  lazy val sbtModuleNamesTask: SbtShellTask[Map[String, String]] = new SbtShellTask[Map[String, String]]:

    override def executeTask(project: Project): Map[String, String] = {
      val comms                    = SbtShellCommunication.forProject(project)
      val moduleIdSbtModuleNameMap = scala.collection.mutable.HashMap[String, String]()
      val outputLines              = ListBuffer[String]()
      val executed = comms.command(
        "moduleName",
        new StringBuilder(),
        SbtShellCommunication.listenerAggregator {
          case SbtShellCommunication.Output(line) =>
            outputLines.append(line.trim)
          case _ =>
        }
      )
      Await.result(executed, 5.minutes)

      val mms = outputLines.filter(_.startsWith("[info]"))
      if (mms.size % 2 == 0) {
        for (i <- 0 until mms.size - 1 by (2)) {
          moduleIdSbtModuleNameMap.put(mms(i).trim, mms(i + 1).trim)
        }
      } else if (mms.size == 1) moduleIdSbtModuleNameMap.put(SingleSbtModule, mms(0))

      moduleIdSbtModuleNameMap.map { (k, v) =>
        val key = k match
          case cmdNameRegex(_, _, moduleName, _, _) => moduleName.trim
          case SingleSbtModule                      => SingleSbtModule
          case _                                    => Constants.Empty_String

        val value = v match
          case shellOutputResultRegex(_, _, sbtModuleName) => sbtModuleName.trim
          case _                                           => Constants.Empty_String

        key -> value

      }.filter(kv => kv._1 != Constants.Empty_String && kv._2 != Constants.Empty_String).toMap
    }

  lazy val organizationTask: SbtShellTask[String] = new SbtShellTask[String]:

    override def executeTask(project: Project): String = {
      val comms       = SbtShellCommunication.forProject(project)
      val outputLines = ListBuffer[String]()
      val executed = comms.command(
        "organization",
        new StringBuilder(),
        SbtShellCommunication.listenerAggregator {
          case SbtShellCommunication.Output(line) =>
            outputLines.append(line.trim)
          case _ =>
        }
      )
      Await.result(executed, 5.minutes)
      outputLines.filter(_.startsWith("[info]")).lastOption.getOrElse("") match
        case shellOutputResultRegex(_, _, org) =>
          org.trim
        case _ => null
    }
}
