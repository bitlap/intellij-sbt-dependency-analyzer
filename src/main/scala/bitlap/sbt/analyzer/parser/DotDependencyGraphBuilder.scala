package bitlap.sbt.analyzer.parser

import bitlap.sbt.analyzer.model.*
import com.intellij.openapi.externalSystem.model.project.dependencies.*

import java.util.{Collections, List as JList}
import scala.jdk.CollectionConverters.*

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/3
 */
object DotDependencyGraphBuilder {
  lazy val instance: DependencyGraphBuilder = new DotDependencyGraphBuilder
}

final class DotDependencyGraphBuilder extends DependencyGraphBuilder {

  override def buildDependencyTree(file: String): JList[DependencyNode] = {
    val tree = buildTree(file)
    tree.asJava
  }

  override def toDependencyNode(dep: Dependency): DependencyNode = {
    if (dep == null) return null
    val node = new ArtifactDependencyNodeImpl(dep.id, dep.group, dep.artifact, dep.version)
    node.setResolutionState(ResolutionState.RESOLVED)
    node
  }

  private def getDependencyRelations(file: String): Option[DependencyRelations] =
    val dependencyGraph = DotUtil.parse(file)
    if (dependencyGraph == null) None
    else
      Some(
        DependencyRelations(
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

  private def buildTree(file: String): Seq[DependencyNode] = {
    val data = getDependencyRelations(file)
    val depMap =
      data.map(_.dependencies.asScala).getOrElse(List.empty).map(d => d.id -> toDependencyNode(d)).toMap

    val relation                  = data.orNull
    
    if(relation == null || relation.relations.size() == 0) return data.map(_.dependencies.asScala.map(d => toDependencyNode(d)).toList).toList.flatten
      
    val parentChildren            = scala.collection.mutable.HashMap[Int, JList[Int]]()
    val graph                     = new Graph(relation.relations.size())
    
    relation.relations.forEach(r => graph.addEdge(r.tail, r.head))
    
    val objs: Seq[DependencyNode] = depMap.values.toSet.toSeq

    objs.foreach { d =>
      val path = graph.DFS(d.getId.toInt).asScala.tail.map(_.intValue()).asJava
      parentChildren.put(d.getId.toInt, path)
    }

    parentChildren.foreach { (k, v) =>
      val kd = depMap.get(k)
      attacheChildNodes(kd, v)
    }

    def attacheChildNodes(node: Option[DependencyNode], vd: JList[Int]): Unit = {
      val rs = vd.asScala.toSet.flatMap(l => depMap.get(l).toList).toList.asJava
      node.map(_.getDependencies.addAll(rs))
    }

    objs

  }
}
