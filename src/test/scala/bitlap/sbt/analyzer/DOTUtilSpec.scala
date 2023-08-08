package bitlap.sbt.analyzer

import java.io.File
import java.util.concurrent.atomic.AtomicLong

import scala.jdk.CollectionConverters.*

import bitlap.sbt.analyzer.model.{ Artifact, DependencyGraph }
import bitlap.sbt.analyzer.model.ArtifactRegex
import bitlap.sbt.analyzer.parser
import bitlap.sbt.analyzer.parser.DOTUtil

import org.scalatest.flatspec.AnyFlatSpec

import guru.nidi.graphviz.attribute.validate.{ ValidatorEngine, ValidatorFormat }
import guru.nidi.graphviz.engine.{ Format, Graphviz }
import guru.nidi.graphviz.model.*
import guru.nidi.graphviz.parse.Parser

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/8
 */
class DOTUtilSpec extends AnyFlatSpec {

  "parse file by Parser" should "ok" in {
    val start                      = System.currentTimeMillis()
    val file                       = getClass.getClassLoader.getResource("test.dot").getFile
    val mutableGraph: MutableGraph = (new Parser).forEngine(ValidatorEngine.DOT).notValidating().read(new File(file))
    // FIXME not use render

    println(s"parse json cost:${System.currentTimeMillis() - start}ms")
    assert(mutableGraph != null)
  }

}
