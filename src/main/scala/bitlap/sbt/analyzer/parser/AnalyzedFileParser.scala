package bitlap.sbt.analyzer.parser

import bitlap.sbt.analyzer.model.*

import com.intellij.buildsystem.model.unified.UnifiedCoordinates
import com.intellij.openapi.externalSystem.model.project.dependencies.*

trait AnalyzedFileParser {

  val fileType: AnalyzedFileType

  def buildDependencyTree(
    context: ModuleContext,
    root: DependencyScopeNode,
    declared: List[UnifiedCoordinates]
  ): DependencyScopeNode

}
