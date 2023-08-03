package bitlap.sbt.analyzer.model

final case class DependencyGraphObject(
  _gvid: Long,
  name: String,
  height: String,
  label: String,
  pos: String,
  width: String
) {

  def toDependency: Dependency = name match
    case DependencyGraphObject.regex(group, artifact, version) => Dependency(_gvid, group, artifact, version)
    case _                                                     => null
}

object DependencyGraphObject {
  val regex = "(.*):(.*):(.*)".r

}
