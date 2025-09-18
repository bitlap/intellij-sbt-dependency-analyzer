package bitlap.sbt.analyzer.parsing

import bitlap.sbt.analyzer.model.*

import com.intellij.openapi.externalSystem.model.project.dependencies.*

trait DependencyGraphParser {

  val dependencyGraphType: DependencyGraphType

  def buildDependencyTree(
    context: ModuleContext,
    root: DependencyScopeNode
  ): DependencyScopeNode

}
