package bitlap.sbt.analyzer.parser

import java.util.{ Collections, List as JList }

import scala.jdk.CollectionConverters.*

import bitlap.sbt.analyzer.DependencyScopeEnum
import bitlap.sbt.analyzer.DependencyScopeEnum.*
import bitlap.sbt.analyzer.DependencyUtil
import bitlap.sbt.analyzer.SbtDependencyAnalyzerContributor.fileName
import bitlap.sbt.analyzer.model.*

import org.jetbrains.plugins.scala.util.ScalaUtil

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.project.dependencies.*

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/3
 */
object DOTDependencyParserBuilder {
  lazy val instance: DependencyParser = new DOTDependencyParserBuilder
}

final class DOTDependencyParserBuilder extends DependencyParser {

  override val parserType: ParserTypeEnum = ParserTypeEnum.DOT

  /** transforming dependencies data into view data
   */
  private def toDependencyNode(context: ModuleContext, dep: Artifact): DependencyNode = {
    if (dep == null) return null
    val node = new ArtifactDependencyNodeImpl(dep.id, dep.group, dep.artifact, dep.version)
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
        .map(_.dependencies.asScala)
        .getOrElse(List.empty)
        .map(d => d.id.toString -> toDependencyNode(context, d))
        .toMap

    val relation = data.orNull

    // if no relations for dependency object
    if (relation == null || relation.relations.size() == 0) return {
      val dep = data.map(_.dependencies.asScala.map(d => toDependencyNode(context, d)).toList).toList.flatten
      root.getDependencies.addAll(dep.filterNot(d => DependencyUtil.filterModuleSelfDependency(d, context)).asJava)
      root
    }
    val relationMap = relation.relations.asScala.map(r => s"${r.head}-${r.tail}" -> r.label).toMap

    val parentChildren = scala.collection.mutable.HashMap[String, JList[Int]]()
    val labelData      = scala.collection.mutable.HashMap[String, String]()
    val tailMax        = relation.relations.asScala.view.map(_.tail).sortWith((a, b) => a > b).headOption.getOrElse(0)
    val headMax        = relation.relations.asScala.view.map(_.head).sortWith((a, b) => a > b).headOption.getOrElse(0)
    val graph          = new Graph(Math.max(tailMax, headMax) + 1)

    // build graph
    relation.relations.forEach { r =>
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
              val lb           = label.getOrElse(child.toString, "")
              val copyArtifact = new ArtifactDependencyNodeImpl(d.getId, d.getGroup, d.getModule, d.getVersion)
              copyArtifact.setResolutionState(d.getResolutionState)
              copyArtifact.setSelectionReason(lb)
              copyArtifact
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
    val dependencyGraph = DOTUtil.parse(file)
    if (dependencyGraph == null) None
    else
      Some(
        Dependencies(
          Option(dependencyGraph.getObjects)
            .getOrElse(Collections.emptyList())
            .asScala
            .map(d => toDependency(d))
            .asJava,
          Option(dependencyGraph.getEdges)
            .getOrElse(Collections.emptyList())
            .asScala
            .map(d => toDependencyRelation(d))
            .asJava
        )
      )

}
