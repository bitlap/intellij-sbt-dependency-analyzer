package bitlap.sbt.analyzer

import java.util
import java.util.concurrent.atomic.AtomicInteger

import scala.jdk.CollectionConverters.*

import bitlap.sbt.analyzer.DependencyUtils
import bitlap.sbt.analyzer.model.*
import bitlap.sbt.analyzer.parser.DOTUtil

import org.scalatest.flatspec.AnyFlatSpec

import guru.nidi.graphviz.model.*

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/8
 */
class DOTUtilSpec extends AnyFlatSpec {

  "parse file as MutableNode" should "ok" in {
    val start = System.currentTimeMillis()
    val file  = getClass.getClassLoader.getResource("test.dot").getFile
    val ctx =
      ModuleContext(
        file,
        "star-authority-protocol",
        DependencyScopeEnum.Compile,
        "fc.xuanwu.star",
        ideaModuleNamePaths = Map.empty,
        isScalaJs = false,
        isScalaNative = false,
        ideaModuleIdSbtModuleNames = Map.empty,
        isTest = true
      )

    val mutableGraph: MutableGraph               = DOTUtil.parseAsGraph(ctx)
    val graphNodes: util.Collection[MutableNode] = mutableGraph.nodes()
    val links: util.Collection[Link]             = mutableGraph.edges()

    val nodes = graphNodes.asScala.map { graphNode =>
      graphNode.name().value() -> DependencyUtils.getArtifactInfoFromDisplayName(None, graphNode.name().value())
    }.collect { case (name, Some(value)) =>
      name -> value
    }.toMap
    val idMapping: Map[String, Int] = nodes.map(kv => kv._2.toString -> kv._2.id)

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
