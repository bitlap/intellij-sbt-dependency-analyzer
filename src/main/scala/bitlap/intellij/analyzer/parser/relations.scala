package bitlap.intellij.analyzer.parser

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/3
 */
final case class DependencyRelations(dependencies: List[Dependency], relations: List[DependencyRelation])

final case class Dependency(id: Long, group: String, artifact: String, version: String)

final case class DependencyRelation(id: Long, tail: Long, head: Long)
