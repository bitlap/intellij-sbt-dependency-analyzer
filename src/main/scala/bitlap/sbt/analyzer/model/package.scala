package bitlap.sbt.analyzer.model

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/3
 */
def toDependencyRelation(edge: DependencyGraphEdge): Relation =
  Relation(edge.tail, edge.head, edge.label)

val ArtifactRegex = "(.*):(.*):(.*)".r

def toDependency(obj: DependencyGraphObject): Artifact = {
  obj.name match
    case ArtifactRegex(group, artifact, version) => Artifact(obj._gvid.toLong, group, artifact, version)
    case _                                       => null
}
