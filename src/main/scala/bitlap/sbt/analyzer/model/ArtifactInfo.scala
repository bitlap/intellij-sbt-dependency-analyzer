package bitlap.sbt.analyzer.model

import bitlap.sbt.analyzer.Constants

final case class ArtifactInfo(id: Int, group: String, artifact: String, version: String) {

  override def toString: String = {
    s"$group${Constants.ARTIFACT_SEPARATOR}$artifact${Constants.ARTIFACT_SEPARATOR}$version"

  }
}
