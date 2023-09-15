package bitlap.sbt.analyzer

import scala.concurrent.duration.*

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/10
 */
object Constants:

  final val LineSeparator: String = "\n"

  final val ColonSeparator: String = ":"
  final val EmptyString: String    = ""

  final val SingleSbtModule = "__Single_Module__"
  final val RootSbtModule   = "__Root_Module__"

  final val Project = "project"

  final val Protobuf = "protobuf"

  final val Timeout = 10.minutes

  final val IntervalTimeout = 1010.milliseconds

  final val ChangeNotesSeparator = "<!-- @@ -->"

end Constants
