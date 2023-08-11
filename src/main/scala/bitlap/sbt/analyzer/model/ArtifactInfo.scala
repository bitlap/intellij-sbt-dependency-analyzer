package bitlap.sbt.analyzer.model

import bitlap.sbt.analyzer.Constants

final case class ArtifactInfo(id: Int, group: String, artifact: String, version: String) {

  override def toString: String = {
    s"${group}${Constants.Colon_Separator}${artifact}${Constants.Colon_Separator}${version}"

  }
}
