package bitlap.sbt.analyzer

enum DependencyScopeEnum:
  // see https://github.com/JetBrains/intellij-scala/blob/idea232.x/sbt/sbt-impl/src/org/jetbrains/sbt/language/utils/SbtDependencyCommon.scala
  case Compile, Provided, Test
