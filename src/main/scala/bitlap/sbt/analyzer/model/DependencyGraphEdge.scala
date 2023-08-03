package bitlap.sbt.analyzer.model

final case class DependencyGraphEdge(
  _gvid: Long,
  tail: Long,
  head: Long,
  arrowtail: String,
  label: String,
  pos: String
) {
  def toDependencyRelation: DependencyRelation = DependencyRelation(_gvid, tail, head)
}
