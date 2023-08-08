package bitlap.sbt.analyzer.parser

import java.util.{ Collections, List as JList }
import java.util.concurrent.atomic.*

import scala.jdk.CollectionConverters.*

import bitlap.sbt.analyzer.DependencyScopeEnum
import bitlap.sbt.analyzer.DependencyScopeEnum.*
import bitlap.sbt.analyzer.DependencyUtil
import bitlap.sbt.analyzer.SbtDependencyAnalyzerContributor.fileName
import bitlap.sbt.analyzer.model.*

import org.jetbrains.plugins.scala.util.ScalaUtil

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.project.dependencies.*

import guru.nidi.graphviz.model.{ Graph as _, * }

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/3
 */
object DOTDependencyParserBuilder {
  lazy val instance: DependencyParser = new DOTDependencyParserBuilder

  final val id = new AtomicInteger(0)

  def artifactAsName(artifact: Artifact): String = {
    s"${artifact.group}:${artifact.artifact}:${artifact.version}"
  }

  def extractArtifactFromName(name: String): Option[Artifact] = {
    name match
      case ArtifactRegex(group, artifact, version) => Some(Artifact(id.incrementAndGet(), group, artifact, version))
      case _                                       => None
  }
}

final class DOTDependencyParserBuilder extends DependencyParser {
  import DOTDependencyParserBuilder.*

  override val parserType: ParserTypeEnum = ParserTypeEnum.DOT

  /** transforming dependencies data into view data
   */
  private def toDependencyNode(context: ModuleContext, dep: Artifact): DependencyNode = {
    if (dep == null) return null
    val node = new ArtifactDependencyNodeImpl(dep.id.toLong, dep.group, dep.artifact, dep.version)
    node.setResolutionState(ResolutionState.RESOLVED)
    node
  }

  /** build tree for dependency analyzer view
   */
  override def buildDependencyTree(context: ModuleContext, root: DependencyScopeNode): DependencyScopeNode = {
    val file = context.analysisFile
    val data = getDependencyRelations(file)
    val depMap =
      data
        .map(_.dependencies)
        .getOrElse(List.empty)
        .map(d => d.id.toString -> toDependencyNode(context, d))
        .toMap

    val relation = data.orNull

    // if no relations for dependency object
    if (relation == null || relation.relations.isEmpty) return {
      val dep = data.map(_.dependencies.map(d => toDependencyNode(context, d)).toList).toList.flatten
      root.getDependencies.addAll(dep.filterNot(d => DependencyUtil.filterModuleSelfDependency(d, context)).asJava)
      root
    }
    val relationMap = relation.relations.map(r => s"${r.head}-${r.tail}" -> r.label).toMap

    val parentChildren = scala.collection.mutable.HashMap[String, JList[Int]]()
    val labelData      = scala.collection.mutable.HashMap[String, String]()
    val tailMax        = relation.relations.view.map(_.tail).sortWith((a, b) => a > b).headOption.getOrElse(0)
    val headMax        = relation.relations.view.map(_.head).sortWith((a, b) => a > b).headOption.getOrElse(0)
    val graph          = new Graph(Math.max(tailMax, headMax) + 1)

    // build graph
    relation.relations.foreach { r =>
      graph.addEdge(r.head, r.tail)
    }

    val objs: Seq[DependencyNode] = depMap.values.toSet.toSeq

    // find children for root nodes
    objs.foreach { d =>
      val path = graph.DFS(d.getId.toInt).asScala.tail.map(_.intValue()).asJava
      parentChildren.put(d.getId.toString, path)
    }

    //
    val filterSelf = objs.filterNot(d => DependencyUtil.filterModuleSelfDependency(d, context))
    filterSelf.foreach { node =>
      val children = parentChildren.getOrElse(node.getId.toString, Collections.emptyList())
      val label    = children.asScala.map(id => id.toString -> relationMap.getOrElse(s"${node.getId}-$id", "")).toMap
      val rs = children.asScala.flatMap { child =>
        depMap
          .get(child.toString)
          .map {
            case d @ (_: ArtifactDependencyNodeImpl) =>
              val lb = label.getOrElse(child.toString, "")
              if (lb != null && lb.nonEmpty) {
                d.setSelectionReason(lb)
              }
              d
            case d @ b => d
          }
          .toList
      }.toList.asJava
      node.getDependencies.addAll(rs)
    }

    root.getDependencies.addAll(filterSelf.asJava)
    root
  }

  /** parse dot file, get graph data
   */
  private def getDependencyRelations(file: String): Option[Dependencies] =
    val mutableGraph: MutableGraph = DOTUtil.parseAsGraph(file)
    if (mutableGraph == null) None
    else
      val graphNodes: java.util.Collection[MutableNode] = mutableGraph.nodes()
      val links: java.util.Collection[Link]             = mutableGraph.edges()

      val nodes = graphNodes.asScala.map { graphNode =>
        graphNode.name().value() -> extractArtifactFromName(graphNode.name().value())
      }.collect { case (name, Some(value)) =>
        name -> value
      }.toMap

      val idMapping: Map[String, Int] = nodes.map(kv => artifactAsName(kv._2) -> kv._2.id)

      val edges = links.asScala.map { l =>
        val label = l.get("label").asInstanceOf[String]
        Relation(
          idMapping.getOrElse(l.from().name().value(), 0),
          idMapping.getOrElse(l.to().name().value(), 0),
          label
        )
      }

      Some(
        Dependencies(
          nodes.values.toList,
          edges.toList
        )
      )

}
