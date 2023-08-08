package bitlap.sbt.analyzer.parser

import java.io.File
import java.util.Collections

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper

import guru.nidi.graphviz.attribute.validate.ValidatorEngine
import guru.nidi.graphviz.engine.*
import guru.nidi.graphviz.engine.{ Format, Graphviz }
import guru.nidi.graphviz.model.MutableGraph
import guru.nidi.graphviz.parse.Parser

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/3
 */
object DOTUtil {

  // not support scala package object in intellij object?
  final lazy val mapper = JsonMapper
    .builder()
    .serializationInclusion(JsonInclude.Include.NON_EMPTY)
    .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
    .build()

  final val totalMemory = 1024 * 1024 * 128

  def parseAsGraph(file: String): MutableGraph = {
    (new Parser).forEngine(ValidatorEngine.DOT).notValidating().read(new File(file))
  }
}
