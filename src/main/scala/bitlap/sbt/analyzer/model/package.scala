package bitlap.sbt.analyzer.model

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/3
 */
def toDependencyRelation(edge: DependencyGraphEdge): DependencyRelation =
  DependencyRelation(edge.tail, edge.head, edge.label)

val regex = "(.*):(.*):(.*)".r

def toDependency(obj: DependencyGraphObject): Dependency = {
  obj.name match
    case regex(group, artifact, version) => Dependency(obj._gvid.toLong, group, artifact, version)
    case _                               => null
}
