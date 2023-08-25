package bitlap.sbt.analyzer.model

import bitlap.sbt.analyzer.DependencyScopeEnum

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/4
 */
final case class ModuleContext(
  analysisFile: String,
  currentModuleId: String,
  scope: DependencyScopeEnum,
  scalaMajor: String,
  org: String,
  ideaModuleNamePaths: Map[String, String] = Map.empty,
  isScalaJs: Boolean = false,
  isScalaNative: Boolean = false,
  ideaModuleIdSbtModuleNames: Map[String, String] = Map.empty
)
