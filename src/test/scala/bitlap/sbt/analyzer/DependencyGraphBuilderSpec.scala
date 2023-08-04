package bitlap.sbt.analyzer

import bitlap.sbt.analyzer.model.ModuleContext
import bitlap.sbt.analyzer.parser.{ DependencyGraphBuilderFactory, GraphBuilderEnum }

import org.scalatest.flatspec.AnyFlatSpec

import com.intellij.openapi.externalSystem.model.project.dependencies.*

class DependencyGraphBuilderSpec extends AnyFlatSpec {

  "parse dot file" should "convert to object successfully " in {
    val root = new DependencyScopeNode(
      0,
      "compile",
      "compile",
      "compile"
    )
    root.setResolutionState(ResolutionState.RESOLVED)

    val ctx =
      ModuleContext(getClass.getClassLoader.getResource("test.dot").getFile, "test", DependencyScopeEnum.Compile)

    val relations = DependencyGraphBuilderFactory
      .getInstance(GraphBuilderEnum.Dot)
      .buildDependencyTree(ctx)

    assert(relations.getDependencies.size() > 0)
  }
}
