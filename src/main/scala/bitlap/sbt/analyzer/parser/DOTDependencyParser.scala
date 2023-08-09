package bitlap.sbt.analyzer.parser

import java.util.{ Collections, List as JList }
import java.util.concurrent.atomic.*

import scala.collection.mutable
import scala.jdk.CollectionConverters.*

import bitlap.sbt.analyzer.DependencyScopeEnum
import bitlap.sbt.analyzer.DependencyScopeEnum.*
import bitlap.sbt.analyzer.DependencyUtil
import bitlap.sbt.analyzer.DependencyUtil.*
import bitlap.sbt.analyzer.SbtDependencyAnalyzerContributor.fileName
import bitlap.sbt.analyzer.model.*

import org.jetbrains.plugins.scala.util.ScalaUtil
import org.jetbrains.sbt.language.utils.SbtDependencyCommon

import com.intellij.buildsystem.model.unified.UnifiedCoordinates
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.project.dependencies.*
import com.intellij.util.containers.InternalIterator.Collector

import guru.nidi.graphviz.model.{ Graph as _, * }

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/3
 */
object DOTDependencyParser {
  lazy val instance: DependencyParser = new DOTDependencyParser

  final val id = new AtomicInteger(0)
}

final class DOTDependencyParser extends DependencyParser {

  private val LOG = Logger.getInstance(classOf[DOTDependencyParser.type])

  import DOTDependencyParser.*

  override val parserType: ParserTypeEnum = ParserTypeEnum.DOT

  /** transforming dependencies data into view data
   */
  private def toDependencyNode(context: ModuleContext, dep: ArtifactInfo): DependencyNode = {
    if (dep == null) return null
    // module dependency
    val node = new ArtifactDependencyNodeImpl(dep.id.toLong, dep.group, dep.artifact, dep.version)
    node.setResolutionState(ResolutionState.RESOLVED)
    node
  }

  /** build tree for dependency analyzer view
   */
  override def buildDependencyTree(
    context: ModuleContext,
    root: DependencyScopeNode,
    declared: List[UnifiedCoordinates]
  ): DependencyScopeNode = {
    val data = getDependencyRelations(context.analysisFile)
    val depMap =
      data.map(_.dependencies.map(a => a.id.toString -> toDependencyNode(context, a)).toMap).getOrElse(Map.empty)

    val relation: Dependencies = data.orNull

    // if no relations for dependency object
    if (relation == null || relation.relations.isEmpty) return {
      val dep         = data.map(_.dependencies.map(d => toDependencyNode(context, d)).toList).toList.flatten
      val excludeSelf = dep.filterNot(d => filterSelfModuleDependency(d, context))
      LOG.info(s"No Relations: $context")
      fixProjectModuleDependencies(root, excludeSelf, context)
      root
    }
    val graph = getGraph(relation, depMap)

    val relationLabelsMap = relation.relations.map { r =>
      // build graph
      graph.addEdge(r.head, r.tail)
      s"${r.head}-${r.tail}" -> r.label
    }.toMap
    val parentChildrenMap = mutable.HashMap[String, JList[Int]]()

    val topLevelNodes: Seq[DependencyNode] = depMap.values.toSet.toSeq

    // find children for root nodes
    topLevelNodes.foreach { d =>
      val path = graph.DFS(d.getId.toInt).asScala.tail.map(_.intValue()).asJava
      parentChildrenMap.put(d.getId.toString, path)
    }

    // ignore self
    val excludeSelf = topLevelNodes.filterNot(d => filterSelfModuleDependency(d, context))
    // append children
    excludeSelf.foreach { node =>
      val children = parentChildrenMap.getOrElse(node.getId.toString, Collections.emptyList()).asScala
      val label    = children.map(id => id.toString -> relationLabelsMap.getOrElse(s"${node.getId}-$id", "")).toMap
      val rs = children.flatMap { child =>
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
      fixProjectModuleDependencies(node, rs.asScala.toSeq, context)
      node.getDependencies.addAll(rs)
    }

    fixProjectModuleDependencies(root, excludeSelf, context)

    // if version is val, we cannot getUnifiedCoordinates from intellij-scala `SbtDependencyUtils.declaredDependencies`
    // So we implement and ignore version number, which may filter multiple libraries from different versions.
    // Considering that we hope to reduce the number of topLevel nodes, this may be acceptable.
    // dropDuplicateNodes(root, declared)
    // TODO Traverse from bottom to top
    root
  }

  private def getGraph(relation: Dependencies, depMap: Map[String, DependencyNode]): Graph = {
    val tailMax = relation.relations.view.map(_.tail).sortWith((a, b) => a > b).headOption.getOrElse(0)
    val headMax = relation.relations.view.map(_.head).sortWith((a, b) => a > b).headOption.getOrElse(0)
    val nodeMax = depMap.keys.view.map(_.toInt).toList.sortWith((a, b) => a > b).headOption.getOrElse(0)
    val graph   = new Graph(Math.max(Math.max(tailMax, headMax), nodeMax) + 1)
    graph
  }

  private def dropDuplicateNodes(dependencyNode: DependencyNode, declared: List[UnifiedCoordinates]): Unit = {
    dependencyNode.getDependencies.removeIf { node =>
      val ret = dependencyNode.getDependencies.asScala.filterNot(_ == node).exists(_.getDependencies.contains(node))
        && !isDeclaredDependencies(declared, node)
      dropDuplicateNodes(node, declared)
      ret
    }
  }

  private def isDeclaredDependencies(declared: List[UnifiedCoordinates], d: DependencyNode): Boolean = {
    declared.exists { uc =>
      if (uc.getVersion == SbtDependencyCommon.defaultLibScope) {
        val artifact = extractArtifactFromName(Some(d.getId.toInt), d.getDisplayName).orNull
        if (artifact == null) false
        else {
          artifact.group == uc.getGroupId && artifact.artifact == uc.getArtifactId
        }
      } else {
        d.getDisplayName == uc.getDisplayName
      }
    }
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
        graphNode.name().value() -> extractArtifactFromName(None, graphNode.name().value())
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
