package bitlap.sbt.analyzer.parser

import bitlap.sbt.analyzer.model.Artifact

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/3
 */
object DependencyParserFactory {

  def getInstance(builder: ParserTypeEnum): DependencyParser = {
    builder match
      case ParserTypeEnum.DOT => DOTDependencyParserBuilder.instance

  }

}
