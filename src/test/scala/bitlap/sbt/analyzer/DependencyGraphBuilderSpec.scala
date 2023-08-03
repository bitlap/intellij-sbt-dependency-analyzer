package bitlap.sbt.analyzer

import bitlap.sbt.analyzer.parser.{ DependencyGraphBuilderFactory, GraphBuilderEnum }

import org.scalatest.flatspec.AnyFlatSpec

class DependencyGraphBuilderSpec extends AnyFlatSpec {

  "parse dot file" should "convert to object successfully " in {

    val relations = DependencyGraphBuilderFactory
      .getInstance(GraphBuilderEnum.Dot)
      .buildDependencyTree(getClass.getClassLoader.getResource("test.dot").getFile)

    assert(relations.size() > 0)
  }
}
