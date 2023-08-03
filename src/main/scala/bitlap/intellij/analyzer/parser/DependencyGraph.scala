package bitlap.intellij.analyzer.parser

final case class DependencyGraph(
  name: String,
  directed: Boolean,
  strict: Boolean,
  bb: String,
  rankdir: String,
  _subgraph_cnt: Long,
  objects: List[DependencyGraphObject],
  edges: List[DependencyGraphEdge]
)
