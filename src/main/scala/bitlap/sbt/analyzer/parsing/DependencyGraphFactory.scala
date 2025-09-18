package bitlap.sbt.analyzer.parsing

object DependencyGraphFactory {

  def getInstance(dependencyGraphType: DependencyGraphType): DependencyGraphParser = {
    dependencyGraphType match
      case DependencyGraphType.Dot => DotDependencyGraphParser.instance
      // TODO
      case DependencyGraphType.GraphML => throw new IllegalArgumentException("Parser type is not supported")
  }

}
