package bitlap.sbt.analyzer

import org.scalatest.flatspec.AnyFlatSpec

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/11
 */
class SbtShellTaskRegex extends AnyFlatSpec {

  "regex match" should "ok" in {
    "[info] \torg.bitlap" match
      case SbtShellTask.shellOutputResultRegex(_, _, org) =>
        assert(org.trim == "org.bitlap")
      case _ => assert(false)

    "[info] rolls-csv / moduleName" match
      case SbtShellTask.cmdNameRegex(_, _, moduleName, _, _) =>
        assert(moduleName.trim == "rolls-csv")
      case _ => assert(false)
  }

}
