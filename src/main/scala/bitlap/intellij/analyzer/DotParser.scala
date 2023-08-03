package bitlap.intellij.analyzer

import java.io.File

import scala.jdk.CollectionConverters.*

import bitlap.intellij.analyzer.parser.*

import com.intellij.openapi.externalSystem.model.project.dependencies.*

import guru.nidi.graphviz.engine.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/3
 */
object DotParser {

  def buildDependencyTree(file: String): java.util.List[DependencyNode] = {
    val (relation, dependencyList) = dependencies(file)
    dependencyList.foreach { dependency =>
      val parentId = dependency.getId
      val children = relation.filter(_.head == parentId).flatMap(f => dependencyList.filter(_.getId == f.id))
      dependency.getDependencies.addAll(children.asJava)
    }

    dependencyList.asJava

  }

  def getDependencyRelations(file: String): Option[DependencyRelations] =
    val dependencyGraph = parse(file)
    if (dependencyGraph == null) None
    else
      Some(
        DependencyRelations(
          dependencyGraph.objects.map(_.toDependency),
          dependencyGraph.edges.map(_.toDependencyRelation)
        )
      )

  def parse(file: String): DependencyGraph = {
    try {
      val string = Graphviz.fromFile(new File(file)).render(Format.JSON0).toString
      parser.parse(string) match
        case Left(value) => null
        case Right(value) =>
          value.as[DependencyGraph] match
            case Left(value)  => null
            case Right(value) => value
    } catch
      case e: Exception =>
        e.printStackTrace()
        null
  }

  private def dependencies(file: String): (List[DependencyRelation], List[DependencyNode]) = {
    val dprs      = getDependencyRelations(file)
    val deps      = dprs.map(_.dependencies).getOrElse(List.empty)
    val relations = dprs.map(_.relations).getOrElse(List.empty)
    relations -> deps.map(toDependencyNode)
  }

  private def toDependencyNode(dep: Dependency): DependencyNode = {
    val node = new ArtifactDependencyNodeImpl(dep.id, dep.group, dep.artifact, dep.version)
    node.setResolutionState(ResolutionState.RESOLVED)
    node
  }

}
