package bitlap
package sbt
package analyzer
package task

import scala.collection.mutable

import com.intellij.openapi.project.Project

import Constants.*

/** Handles the `sbt moduleName` command to retrieve all module names within an sbt project. This specifically refers to
 *  modules declared via `name := ` in the `build.sbt` file, as opposed to those defined within IntelliJ IDEA's project
 *  structure.
 */
final class ModuleNameTask extends SbtShellOutputAnalysisTask[Map[String, String]]:
  import SbtShellOutputAnalysisTask.*

  /** {{{
   *    lazy val `rolls` = (project in file("."))
   *  .aggregate(
   *  `rolls-compiler-plugin`,
   *  `rolls-core`,
   *  `rolls-csv`,
   *  `rolls-zio`,
   *  `rolls-plugin-tests`,
   *  `rolls-docs`
   *  )
   *  }}}
   *  The `rolls-docs` module cannot be processed unless it is explicitly aggregated within the build configuration.
   *
   *  TODO support executing the command for a single module: `rolls-docs / moduleName` to get module Name
   */
  override def executeCommand(project: Project): Map[String, String] =
    val mms                      = getCommandOutputLines(project, "moduleName")
    val moduleIdSbtModuleNameMap = mutable.HashMap[String, String]()
    if ((mms.size & 1) == 0) {
      for (i <- 0 until mms.size - 1 by 2) {
        moduleIdSbtModuleNameMap.put(mms(i).trim, mms(i + 1).trim)
      }
    } else if (mms.size == 1) moduleIdSbtModuleNameMap.put(SINGLE_SBT_MODULE, mms.head)

    moduleIdSbtModuleNameMap.map { (k, v) =>
      val key = k match
        case MODULE_NAME_INPUT_REGEX(_, _, moduleName, _, _) => moduleName.trim
        case ROOT_MODULE_NAME_INPUT_REGEX(_, _)              => ROOT_SBT_MODULE
        case SINGLE_SBT_MODULE                               => SINGLE_SBT_MODULE
        case _                                               => EMPTY_STRING

      val value = v match
        case SHELL_OUTPUT_RESULT_REGEX(_, _, sbtModuleName) => sbtModuleName.trim
        case _                                              => EMPTY_STRING

      key -> value

    }.filter(kv => kv._1 != EMPTY_STRING && kv._2 != EMPTY_STRING).toMap

  end executeCommand

end ModuleNameTask
