package bitlap.sbt.analyzer.parser

import java.io.File

import scala.util.Try

import guru.nidi.graphviz.attribute.validate.ValidatorEngine
import guru.nidi.graphviz.model.MutableGraph
import guru.nidi.graphviz.parse.Parser

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/3
 */
object DOTUtil {

  def parseAsGraph(file: String): MutableGraph = {
    Try((new Parser).forEngine(ValidatorEngine.DOT).notValidating().read(new File(file))).getOrElse(null)
  }
}
