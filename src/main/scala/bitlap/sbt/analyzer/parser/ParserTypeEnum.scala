package bitlap.sbt.analyzer.parser

enum ParserTypeEnum(val cmd: String, val suffix: String) {
  case DOT extends ParserTypeEnum("dependencyDot", "dot")
}
