package bitlap.sbt.analyzer.parser

import java.io.File

import scala.jdk.CollectionConverters.*

import bitlap.sbt.analyzer.model.*
import bitlap.sbt.analyzer.model.{ Dependency, DependencyRelation, DependencyRelations }

import com.intellij.openapi.externalSystem.model.project.dependencies.*

import guru.nidi.graphviz.engine.*
import guru.nidi.graphviz.engine.{ Format, Graphviz }
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/3
 */
object DotDependencyGraphBuilder {
  lazy val instance: DependencyGraphBuilder = new DotDependencyGraphBuilder
}

final class DotDependencyGraphBuilder extends DependencyGraphBuilder {

  override def buildDependencyTree(file: String): java.util.List[DependencyNode] = {
    val (relation, dependencyList) = dependencies(file)
    dependencyList.foreach { dependency =>
      val parentId = dependency.getId
      val children = relation.filter(_.head == parentId).flatMap(f => dependencyList.filter(_.getId == f.id))
      dependency.getDependencies.addAll(children.asJava)
    }

    dependencyList.asJava
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
          dependencyGraph.objects.map(_.toDependency),
          dependencyGraph.edges.map(_.toDependencyRelation)
        )
      )

  private def dependencies(file: String): (List[DependencyRelation], List[DependencyNode]) = {
    val dprs      = getDependencyRelations(file)
    val deps      = dprs.map(_.dependencies).getOrElse(List.empty)
    val relations = dprs.map(_.relations).getOrElse(List.empty)
    relations -> deps.map(toDependencyNode)
  }

}
