package bitlap.sbt.analyzer

import bitlap.sbt.analyzer.task.SbtShellOutputAnalysisTask

import org.scalatest.flatspec.AnyFlatSpec

class SbtShellTaskRegex extends AnyFlatSpec {

  "regex match" should "ok" in {
    "[info] \torg.bitlap" match
      case SbtShellOutputAnalysisTask.shellOutputResultRegex(_, _, org) =>
        assert(org.trim == "org.bitlap")
      case _ => assert(false)

    "[info] rolls-csv / moduleName" match
      case SbtShellOutputAnalysisTask.moduleNameInputRegex(_, _, moduleName, _, _) =>
        assert(moduleName.trim == "rolls-csv")
      case _ => assert(false)

    "[info] moduleName" match
      case SbtShellOutputAnalysisTask.rootModuleNameInputRegex(_, _) =>
        assert(true)
      case _ => assert(false)

    "[info] discovery / libraryDependencies" match
      case SbtShellOutputAnalysisTask.libraryDependenciesInputRegex(_, _, module, _, _) =>
        assert(module.trim == "discovery")
      case _ => assert(false)

    "[info] libraryDependencies" match
      case SbtShellOutputAnalysisTask.rootLibraryDependenciesInputRegex(_, _) =>
        assert(true)
      case _ => assert(false)

    "List(org.scala-lang:scala-library:2.13.11, junit:junit:4.13.2:test, org.scalatest:scalatest:3.2.14:test)" match
      case SbtShellOutputAnalysisTask.libraryDependenciesOutputRegex(modules) =>
        assert(
          modules == "org.scala-lang:scala-library:2.13.11, junit:junit:4.13.2:test, org.scalatest:scalatest:3.2.14:test"
        )
      case _ => assert(false)

    "[info] * org.scalatest:scalatest:3.2.18:test" match
      case SbtShellOutputAnalysisTask.shellOutputStarResultRegex(_, _, _, _, artifact) =>
        assert(artifact == "org.scalatest:scalatest:3.2.18:test")
      case _ => assert(false)

    "org.scalatest:scalatest:3.2.17" match
      case SbtShellOutputAnalysisTask.libraryDependenciesOutput1(group, _, _) =>
        assert(group == "org.scalatest")
      case _ => assert(false)

    "org.scalatest:scalatest:3.2.18:optional" match
      case SbtShellOutputAnalysisTask.libraryDependenciesOutput2(group, _, _, _) =>
        assert(group == "org.scalatest")
      case _ => assert(false)

    "org.scalatest:scalatest:3.2.18:test" match
      case SbtShellOutputAnalysisTask.libraryDependenciesOutput2(group, _, _, _) =>
        assert(group == "org.scalatest")
      case _ => assert(false)

    "org.scalatest:scalatest:3.2.18:test force" match
      case SbtShellOutputAnalysisTask.libraryDependenciesOutput4(group, _, _, _, _, _) =>
        assert(group == "org.scalatest")
      case _ => assert(false)

    "org.scalatest:scalatest:3.2.18:optional;provided" match
      case SbtShellOutputAnalysisTask.libraryDependenciesOutput3(group, _, _, opt, _) =>
        assert(group == "org.scalatest")
        assert(opt == "optional")
      case _ => assert(false)
  }

}
