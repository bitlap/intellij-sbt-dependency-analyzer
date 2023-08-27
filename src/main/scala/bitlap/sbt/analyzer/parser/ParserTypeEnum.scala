package bitlap.sbt.analyzer.parser

enum ParserTypeEnum(val cmd: String, val suffix: String) {
  case DOT     extends ParserTypeEnum("dependencyDot", "dot")
  case GraphML extends ParserTypeEnum("dependencyGraphML", "graphml")
}
