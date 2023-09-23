package bitlap.sbt.analyzer.parser

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/3
 */
object AnalyzedParserFactory {

  def getInstance(builder: AnalyzedFileType): AnalyzedFileParser = {
    builder match
      case AnalyzedFileType.Dot => AnalyzedDotFileParser.instance
      // TODO
      case AnalyzedFileType.GraphML => throw new IllegalArgumentException("Parser type is not supported")
  }

}
