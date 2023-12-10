package bitlap.sbt.analyzer.parser

object AnalyzedParserFactory {

  def getInstance(builder: AnalyzedFileType): AnalyzedFileParser = {
    builder match
      case AnalyzedFileType.Dot => AnalyzedDotFileParser.instance
      // TODO
      case AnalyzedFileType.GraphML => throw new IllegalArgumentException("Parser type is not supported")
  }

}
