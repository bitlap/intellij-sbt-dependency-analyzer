package bitlap.sbt.analyzer

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/3
 */
enum DependencyScopeEnum:
  // see https://github.com/JetBrains/intellij-scala/blob/idea232.x/sbt/sbt-impl/src/org/jetbrains/sbt/language/utils/SbtDependencyCommon.scala
  case Compile, Provided, Test
