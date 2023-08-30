package bitlap.sbt.analyzer.parser

import java.util.List as JList
import java.util.concurrent.atomic.*

import scala.collection.mutable
import scala.jdk.CollectionConverters.*

import bitlap.sbt.analyzer.DependencyUtils
import bitlap.sbt.analyzer.DependencyUtils.*
import bitlap.sbt.analyzer.model.*

import org.jetbrains.sbt.language.utils.SbtDependencyCommon

import com.intellij.buildsystem.model.unified.UnifiedCoordinates
import com.intellij.openapi.externalSystem.model.project.dependencies.*

import guru.nidi.graphviz.model.{ Graph as _, * }

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/3
 */
object DOTDependencyParser:
  lazy val instance: DependencyParser = new DOTDependencyParser

  final val id = new AtomicInteger(0)

end DOTDependencyParser

final class DOTDependencyParser extends DependencyParser:

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
    val data = getDependencyRelations(context)
    val depMap =
      data.map(_.dependencies.map(a => a.id.toString -> toDependencyNode(context, a)).toMap).getOrElse(Map.empty)

    val dependencies: Dependencies = data.orNull

    // if no relations for dependency object
    if (dependencies == null || dependencies.relations.isEmpty) return {
      val dep             = data.map(_.dependencies.map(d => toDependencyNode(context, d)).toList).toList.flatten
      val excludeSelfNode = dep.filterNot(d => isSelfModule(d, context))
      appendChildrenAndFixProjectNodes(root, excludeSelfNode, context)
      root
    }
    val graph = getGraph(dependencies, depMap)

    val relationLabelsMap = dependencies.relations.map { r =>
      // build graph
      graph.addEdge(r.head, r.tail)
      s"${r.head}-${r.tail}" -> r.label
    }.toMap
    val parentChildrenMap = mutable.HashMap[String, JList[Int]]()

    // find children all nodes nodes,there may be indirect dependencies here.
    depMap.values.toSet.toSeq.foreach { topNode =>
      val path = graph
        .DFS(topNode.getId.toInt)
        .asScala
        .tail
        .map(_.intValue())
        .filter(childId => filterOnlyDirectlyChild(topNode, childId, dependencies.relations))
        .asJava
      parentChildrenMap.put(topNode.getId.toString, path)
    }

    // get self
    val selfNode = depMap.values.toSet.toSeq.filter(d => isSelfModule(d, context))
    // append children for self
    selfNode.foreach { node =>
      toNodes(node, parentChildrenMap, depMap, relationLabelsMap, context, dependencies.relations)
    }

    // transfer from self to root
    selfNode.foreach(d => root.getDependencies.addAll(d.getDependencies))
    root
  }

  /** This is important to filter out non direct dependencies
   */
  private def filterOnlyDirectlyChild(parent: DependencyNode, childId: Int, relations: List[Relation]) = {
    relations.exists(r => r.head == parent.getId && r.tail == childId)
  }

  /** Recursively create and add child nodes to root
   */
  private def toNodes(
    parentNode: DependencyNode,
    parentChildrenMap: mutable.HashMap[String, JList[Int]],
    depMap: Map[String, DependencyNode],
    relationLabelsMap: Map[String, String],
    context: ModuleContext,
    relations: List[Relation]
  ): Unit = {
    val childIds = parentChildrenMap
      .get(parentNode.getId.toString)
      .map(_.asScala.toList)
      .getOrElse(List.empty)
      .filter(cid => filterOnlyDirectlyChild(parentNode, cid, relations))
    if (childIds.isEmpty) return
    val childNodes = childIds.flatMap { id =>
      depMap
        .get(id.toString)
        .map {
          case d @ (_: ArtifactDependencyNodeImpl) =>
            val label   = relationLabelsMap.getOrElse(s"${parentNode.getId}-$id", "")
            val newNode = new ArtifactDependencyNodeImpl(d.getId, d.getGroup, d.getModule, d.getVersion)
            if (label != null && label.nonEmpty) {
              newNode.setSelectionReason(label)
            }
            newNode.setResolutionState(d.getResolutionState)
            newNode
          case d @ b => d
        }
        .toList
    }
    childNodes.foreach(d => toNodes(d, parentChildrenMap, depMap, relationLabelsMap, context, relations))
    appendChildrenAndFixProjectNodes(parentNode, childNodes, context)
  }

  private def getGraph(relation: Dependencies, depMap: Map[String, DependencyNode]): Graph = {
    val tailMax = relation.relations.view.map(_.tail).sortWith((a, b) => a > b).headOption.getOrElse(0)
    val headMax = relation.relations.view.map(_.head).sortWith((a, b) => a > b).headOption.getOrElse(0)
    val nodeMax = depMap.keys.view.map(_.toInt).toList.sortWith((a, b) => a > b).headOption.getOrElse(0)
    val graph   = new Graph(Math.max(Math.max(tailMax, headMax), nodeMax) + 1)
    graph
  }

  // if version is val, we cannot getUnifiedCoordinates from intellij-scala `SbtDependencyUtils.declaredDependencies`
  // So we implement and ignore version number, which may filter multiple libraries from different versions.
  // Considering that we hope to reduce the number of topLevel nodes, this may be acceptable.
  private def isDeclaredDependencies(declared: List[UnifiedCoordinates], d: DependencyNode): Boolean = {
    declared.exists { uc =>
      if (uc.getVersion == SbtDependencyCommon.defaultLibScope) {
        val artifact = getArtifactInfoFromDisplayName(Some(d.getId.toInt), d.getDisplayName).orNull
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
  private def getDependencyRelations(context: ModuleContext): Option[Dependencies] =
    val mutableGraph: MutableGraph = DOTUtil.parseAsGraph(context)
    if (mutableGraph == null) None
    else
      val graphNodes: java.util.Collection[MutableNode] = mutableGraph.nodes()
      val links: java.util.Collection[Link]             = mutableGraph.edges()

      val nodes = graphNodes.asScala.map { graphNode =>
        graphNode.name().value() -> getArtifactInfoFromDisplayName(None, graphNode.name().value())
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

      Some(
        Dependencies(
          nodes.values.toList,
          edges.toList
        )
      )

end DOTDependencyParser
