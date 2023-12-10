package bitlap.sbt.analyzer.model

import bitlap.sbt.analyzer.DependencyScopeEnum

final case class ModuleContext(
  analysisFile: String,
  currentModuleId: String,
  scope: DependencyScopeEnum,
  organization: String,
  ideaModuleNamePaths: Map[String, String] = Map.empty,
  isScalaJs: Boolean = false,
  isScalaNative: Boolean = false,
  ideaModuleIdSbtModuleNames: Map[String, String] = Map.empty,
  isTest: Boolean = false
)
