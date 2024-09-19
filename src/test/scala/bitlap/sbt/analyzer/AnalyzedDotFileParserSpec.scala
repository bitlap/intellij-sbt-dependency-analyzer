package bitlap.sbt.analyzer

import bitlap.sbt.analyzer.model.ModuleContext
import bitlap.sbt.analyzer.parser.{ AnalyzedFileType, AnalyzedParserFactory }

import org.scalatest.flatspec.AnyFlatSpec

import com.intellij.openapi.externalSystem.model.project.dependencies.*

class AnalyzedDotFileParserSpec extends AnyFlatSpec {

  "parse dot file" should "convert to object successfully " in {
    val start = System.currentTimeMillis()

    val root = new DependencyScopeNode(
      0,
      "compile",
      "compile",
      "compile"
    )
    root.setResolutionState(ResolutionState.RESOLVED)

    val ctx =
      ModuleContext(
        getClass.getClassLoader.getResource("test.dot").getFile,
        "star-authority-protocol",
        DependencyScopeEnum.Compile,
        "fc.xuanwu.star",
        ideaModuleNamePaths = Map.empty,
        ideaModuleIdSbtModuleNames = Map.empty,
        isTest = true
      )

    val relations = AnalyzedParserFactory
      .getInstance(AnalyzedFileType.Dot)
      .buildDependencyTree(ctx, root)

    println(s"analyze dot cost:${System.currentTimeMillis() - start}ms")

    assert(relations.getDependencies.size() > 0)
  }
}
