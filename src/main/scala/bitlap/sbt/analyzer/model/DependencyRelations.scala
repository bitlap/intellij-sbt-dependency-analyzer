package bitlap.sbt.analyzer.model

import java.util.List as JList

final case class DependencyRelations(dependencies: JList[Dependency], relations: JList[DependencyRelation])
