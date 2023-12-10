package bitlap.sbt.analyzer.model

import bitlap.sbt.analyzer.DependencyScopeEnum

sealed abstract class AnalyzerException(msg: String)           extends RuntimeException(msg)
final case class AnalyzerCommandNotFoundException(msg: String) extends AnalyzerException(msg)

final case class AnalyzerCommandUnknownException(
  command: String,
  moduleId: String,
  scope: DependencyScopeEnum,
  msg: String
) extends AnalyzerException(msg)
