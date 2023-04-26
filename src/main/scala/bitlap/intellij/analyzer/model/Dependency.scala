package bitlap.intellij.analyzer.model

import org.jetbrains.sbt.language.utils.*

/** @author
 *    梦境迷离
 *  @version 1.0,2023/4/26
 */
final case class Dependency(sbtArtifactInfo: SbtArtifactInfo):

  var highlight: Boolean = false

  val artifactId: String = sbtArtifactInfo.artifactId

  val groupId: String = sbtArtifactInfo.groupId

  val version: String = sbtArtifactInfo.version

  override def toString: String = sbtArtifactInfo.artifactId
