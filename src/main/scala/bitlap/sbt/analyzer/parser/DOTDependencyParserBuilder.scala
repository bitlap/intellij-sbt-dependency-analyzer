package bitlap.sbt.analyzer.parser

import java.util.{ Collections, List as JList }

import scala.jdk.CollectionConverters.*

import bitlap.sbt.analyzer.DependencyScopeEnum
import bitlap.sbt.analyzer.DependencyScopeEnum.*
import bitlap.sbt.analyzer.DependencyUtil
import bitlap.sbt.analyzer.SbtDependencyAnalyzerContributor.fileName
import bitlap.sbt.analyzer.model.*

import org.jetbrains.plugins.scala.util.ScalaUtil

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
      data.map(_.dependencies.asScala).getOrElse(List.empty).map(d => d.id -> toDependencyNode(context, d)).toMap

    val relation = data.orNull

    if (relation == null || relation.relations.size() == 0) return {
      val dep = data.map(_.dependencies.asScala.map(d => toDependencyNode(context, d)).toList).toList.flatten
      root.getDependencies.addAll(dep.filterNot(d => DependencyUtil.filterModuleSelfDependency(d, context)).asJava)
      root
    }

    val parentChildren = scala.collection.mutable.HashMap[Int, JList[Int]]()
    val labelData      = scala.collection.mutable.HashMap[String, String]()
    val tailMax        = relation.relations.asScala.view.map(_.tail).sortWith((a, b) => a > b).headOption.getOrElse(0)
    val headMax        = relation.relations.asScala.view.map(_.head).sortWith((a, b) => a > b).headOption.getOrElse(0)
    val graph          = new Graph(Math.max(tailMax, headMax) + 1)

    relation.relations.forEach { r =>
      labelData.put(s"${r.tail}-${r.head}", r.label)
      graph.addEdge(r.tail, r.head)
    }

    val objs: Seq[DependencyNode] = depMap.values.toSet.toSeq

    objs.foreach { d =>
      val path = graph.DFS(d.getId.toInt).asScala.tail.map(_.intValue()).asJava
      parentChildren.put(d.getId.toInt, path)
    }

    parentChildren.foreach { (k, v) =>
      val node  = depMap.get(k)
      val label = v.asScala.map(id => id -> labelData.get(s"$k-$id")).toMap
      val rs = v.asScala.view.toSet.flatMap { l =>
        depMap.get(l).toList.map {
          case a: ArtifactDependencyNodeImpl =>
            a.setSelectionReason(label.getOrElse(l, None).getOrElse(""))
            a
          case b => b
        }
      }.toList.asJava
      node.map(_.getDependencies.addAll(rs))
    }

    root.getDependencies.addAll(objs.filterNot(d => DependencyUtil.filterModuleSelfDependency(d, context)).asJava)
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
