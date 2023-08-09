package bitlap.sbt.analyzer.model

import bitlap.sbt.analyzer.DependencyScopeEnum

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/4
 */
final case class ModuleContext(
  analysisFile: String,
  currentModuleName: String,
  scope: DependencyScopeEnum,
  scalaMajor: String,
  org: String,
  allModulePaths: Map[String, String] = Map.empty
)

object ModuleContext {
  val SELF = "SELF"
}
