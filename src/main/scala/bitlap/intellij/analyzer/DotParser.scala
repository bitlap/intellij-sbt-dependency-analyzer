package bitlap.intellij.analyzer

import java.io.File

import bitlap.intellij.analyzer.DotParser
import bitlap.intellij.analyzer.parser.*

import guru.nidi.graphviz.engine.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/3
 */
object DotParser {

  def getDependencyRelations(file: String): Option[DependencyRelations] =
    val dependencyGraph = parse(file)
    if (dependencyGraph == null) None
    else
      Some(
        DependencyRelations(
          dependencyGraph.objects.map(_.toDependency),
          dependencyGraph.edges.map(_.toDependencyRelation)
        )
      )

  def parse(file: String): DependencyGraph = {
    try {
      val string = Graphviz.fromFile(new File(file)).render(Format.JSON0).toString
      parser.parse(string) match
        case Left(value) => null
        case Right(value) =>
          value.as[DependencyGraph] match
            case Left(value)  => null
            case Right(value) => value
    } catch
      case e: Exception =>
        e.printStackTrace()
        null
  }

}
