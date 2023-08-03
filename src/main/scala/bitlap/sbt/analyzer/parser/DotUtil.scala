package bitlap.sbt.analyzer.parser

import java.io.File
import bitlap.sbt.analyzer.model.DependencyGraph
import guru.nidi.graphviz.engine.{ Format, Graphviz, GraphvizLoader }
import io.circe.generic.auto.*
import io.circe.parser

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/3
 */
object DotUtil {

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
