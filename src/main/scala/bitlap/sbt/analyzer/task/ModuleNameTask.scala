package bitlap
package sbt
package analyzer
package task

import scala.collection.mutable

import com.intellij.openapi.project.Project

import Constants.*

/** Process the `sbt moduleName` command, get all module names in sbt, it refers to the module name declared through
 *  `name =: ` in `build.sbt` instead of Intellij IDEA.
 *  @author
 *    梦境迷离
 *  @version 1.0,2023/8/19
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
   *  at present, if do not `aggregate` module rolls-docs, module rolls-docs cannot be analyzed.
   *
   *  TODO fallback, exec cmd for single module: `rolls-docs / moduleName` to get module Name
   *
   *  @param project
   *  @return
   */
  override def executeCommand(project: Project): Map[String, String] =
    val mms                      = getCommandOutputLines(project, "moduleName")
    val moduleIdSbtModuleNameMap = mutable.HashMap[String, String]()
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
        case _                                            => EmptyString

      val value = v match
        case shellOutputResultRegex(_, _, sbtModuleName) => sbtModuleName.trim
        case _                                           => EmptyString

      key -> value

    }.filter(kv => kv._1 != EmptyString && kv._2 != EmptyString).toMap

  end executeCommand

end ModuleNameTask
