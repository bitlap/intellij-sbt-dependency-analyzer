package bitlap.sbt.analyzer.model

import java.util.List as JList

final case class Dependencies(dependencies: JList[Artifact], relations: JList[Relation])
