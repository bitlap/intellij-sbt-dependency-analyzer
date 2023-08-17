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
      case SbtShellTask.moduleNameInputRegex(_, _, moduleName, _, _) =>
        assert(moduleName.trim == "rolls-csv")
      case _ => assert(false)

    "[info] moduleName" match
      case SbtShellTask.rootModuleNameInputRegex(_, _) =>
        assert(true)
      case _ => assert(false)

    "[info] discovery / libraryDependencies" match
      case SbtShellTask.libraryDependenciesInputRegex(_, _, module, _, _) =>
        assert(module.trim == "discovery")
      case _ => assert(false)

    "[info] libraryDependencies" match
      case SbtShellTask.rootLibraryDependenciesInputRegex(_, _) =>
        assert(true)
      case _ => assert(false)

    "List(org.scala-lang:scala-library:2.13.11, junit:junit:4.13.2:test, org.scalatest:scalatest:3.2.14:test)" match
      case SbtShellTask.libraryDependenciesOutputRegex(modules) =>
        assert(
          modules == "org.scala-lang:scala-library:2.13.11, junit:junit:4.13.2:test, org.scalatest:scalatest:3.2.14:test"
        )
      case _ => assert(false)

    "[info] * org.scalatest:scalatest:3.2.16:test" match
      case SbtShellTask.shellOutputStarResultRegex(_, _, _, _, artifact) =>
        assert(artifact == "org.scalatest:scalatest:3.2.16:test")
      case _ => assert(false)

    "org.scalatest:scalatest:3.2.16" match
      case SbtShellTask.libraryDependenciesOutput1(group, _, _) =>
        assert(group == "org.scalatest")
      case _ => assert(false)

    "org.scalatest:scalatest:3.2.16:optional" match
      case SbtShellTask.libraryDependenciesOutput2(group, _, _, _) =>
        assert(group == "org.scalatest")
      case _ => assert(false)

    "org.scalatest:scalatest:3.2.16:test" match
      case SbtShellTask.libraryDependenciesOutput2(group, _, _, _) =>
        assert(group == "org.scalatest")
      case _ => assert(false)

    "org.scalatest:scalatest:3.2.16:test force" match
      case SbtShellTask.libraryDependenciesOutput4(group, _, _, _, _, _) =>
        assert(group == "org.scalatest")
      case _ => assert(false)

    "org.scalatest:scalatest:3.2.16:optional;provided" match
      case SbtShellTask.libraryDependenciesOutput3(group, _, _, opt, _) =>
        assert(group == "org.scalatest")
        assert(opt == "optional")
      case _ => assert(false)
  }

}
