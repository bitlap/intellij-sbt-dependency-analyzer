package bitlap.sbt.analyzer

import java.io.File
import java.util
import java.util.concurrent.atomic.{ AtomicInteger, AtomicLong }

import scala.collection.mutable
import scala.jdk.CollectionConverters.*

import bitlap.sbt.analyzer.model.*
import bitlap.sbt.analyzer.parser
import bitlap.sbt.analyzer.parser.{ DOTDependencyParserBuilder, DOTUtil }

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

  val id = new AtomicInteger(0)

  "parse file as MutableNode" should "ok" in {
    val start                                    = System.currentTimeMillis()
    val file                                     = getClass.getClassLoader.getResource("test.dot").getFile
    val mutableGraph: MutableGraph               = DOTUtil.parseAsGraph(file)
    val graphNodes: util.Collection[MutableNode] = mutableGraph.nodes()
    val links: util.Collection[Link]             = mutableGraph.edges()

    val nodes = graphNodes.asScala.map { graphNode =>
      graphNode.name().value() -> DOTDependencyParserBuilder.extractArtifactFromName(graphNode.name().value())
    }.collect { case (name, Some(value)) =>
      name -> value
    }.toMap
    val idMapping: Map[String, Int] = nodes.map(kv => DOTDependencyParserBuilder.artifactAsName(kv._2) -> kv._2.id)

    val edges = links.asScala.map { l =>
      val label = l.get("label").asInstanceOf[String]
      Relation(
        idMapping.getOrElse(l.from().name().value(), 0),
        idMapping.getOrElse(l.to().name().value(), 0),
        label
      )
    }

    println(s"parse dot cost:${System.currentTimeMillis() - start}ms")
    assert(nodes.size == 69)
    assert(edges.size == 146)

  }

}
