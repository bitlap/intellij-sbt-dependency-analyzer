package bitlap.sbt.analyzer.parser

import java.io.File
import java.util
import java.util.Collections
import java.util.List as JList

import scala.jdk.CollectionConverters.*

import bitlap.sbt.analyzer.model.*
import bitlap.sbt.analyzer.model.{ Dependency, DependencyRelation, DependencyRelations }

import com.intellij.openapi.externalSystem.model.project.dependencies.*

import guru.nidi.graphviz.engine.*
import guru.nidi.graphviz.engine.{ Format, Graphviz }

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/3
 */
object DotDependencyGraphBuilder {
  lazy val instance: DependencyGraphBuilder = new DotDependencyGraphBuilder
}

final class DotDependencyGraphBuilder extends DependencyGraphBuilder {

  val visited = scala.collection.mutable.HashMap[Long, Dependency]()

  override def buildDependencyTree(file: String): JList[DependencyNode] = {
    val (relation, dependencyList) = dependencies(file)
    dependencyList.forEach {
      case dependency: ArtifactDependencyNodeImpl =>
        val parentId = dependency.getId
        val children =
          relation.asScala.filter(_.head == parentId).flatMap(f => dependencyList.asScala.filter(_.getId == f.id))
        dependency.getDependencies.addAll(children.asJava)
      case _ =>
    }

    dependencyList
  }

  override def toDependencyNode(dep: Dependency): DependencyNode = {
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

  private def dependencies(file: String): (JList[DependencyRelation], JList[DependencyNode]) = {
    val dprs                          = getDependencyRelations(file)
    val deps                          = dprs.map(_.dependencies.asScala).getOrElse(List.empty)
    val relations                     = dprs.map(_.relations.asScala).getOrElse(List.empty)
    val rs: JList[DependencyRelation] = new util.ArrayList[DependencyRelation]()
    rs.addAll(relations.asJava)
    val ds: JList[DependencyNode] = new util.ArrayList[DependencyNode]()
    ds.addAll(deps.map { d =>
      toDependencyNode(d)
    }.asJava)
    rs -> ds
  }

}
