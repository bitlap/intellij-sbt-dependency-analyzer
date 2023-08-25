package bitlap.sbt.analyzer.model

import bitlap.sbt.analyzer.DependencyScopeEnum

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/25
 */
sealed abstract class AnalyzerException(msg: String)           extends RuntimeException(msg)
final case class AnalyzerCommandNotFoundException(msg: String) extends AnalyzerException(msg)

final case class AnalyzerCommandUnknownException(
  command: String,
  moduleId: String,
  scope: DependencyScopeEnum,
  msg: String
) extends AnalyzerException(msg)
