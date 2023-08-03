package bitlap.sbt.analyzer.parser

import java.io.File

import bitlap.sbt.analyzer.model.DependencyGraph

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper

import guru.nidi.graphviz.engine.{ Format, Graphviz }

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/3
 */
object DotUtil {

  // not support scala package object in intellij object?
  final lazy val mapper = JsonMapper
    .builder()
    .serializationInclusion(JsonInclude.Include.NON_EMPTY)
    .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
    .build()

  def parse(file: String): DependencyGraph = {
    try {
      val string = Graphviz.fromFile(new File(file)).render(Format.JSON0).toString

      mapper.readValue(string, classOf[DependencyGraph])
    } catch
      case e: Exception =>
        e.printStackTrace()
        null
  }
}
