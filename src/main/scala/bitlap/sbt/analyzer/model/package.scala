package bitlap.sbt.analyzer.model

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/3
 */
def toDependencyRelation(edge: DependencyGraphEdge): DependencyRelation =
  DependencyRelation(edge._gvid, edge.tail, edge.head)

val regex = "(.*):(.*):(.*)".r

def toDependency(obj: DependencyGraphObject): Dependency = {
  obj.name match
    case regex(group, artifact, version) => Dependency(obj._gvid, group, artifact, version)
    case _                               => null
}
