package bitlap.sbt.analyzer

import bitlap.sbt.analyzer.model.ModuleContext
import bitlap.sbt.analyzer.parser.{ AnalyzedFileType, AnalyzedParserFactory }

import org.scalatest.flatspec.AnyFlatSpec

import com.intellij.openapi.externalSystem.model.project.dependencies.*

class AnalyzedDotFileParserSpec extends AnyFlatSpec {

  "parse dot file" should "convert to object successfully " in {
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
      .buildDependencyTree(ctx, root, List.empty)

    assert(relations.getDependencies.size() > 0)
  }
}
