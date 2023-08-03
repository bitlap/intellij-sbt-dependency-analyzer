package bitlap.sbt.analyzer.parser

import bitlap.sbt.analyzer.model.Dependency

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/3
 */
object DependencyGraphBuilderFactory {

  def getInstance(builder: GraphBuilderEnum): DependencyGraphBuilder = {
    builder match
      case GraphBuilderEnum.Dot => DotDependencyGraphBuilder.instance

  }

}
