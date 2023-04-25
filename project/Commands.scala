import sbt.Command

/** @author
 *    梦境迷离
 *  @since 2023/4/25
 *  @version 1.0
 */
object Commands {

  val FmtSbtCommand = Command.command("fmt")(state => "scalafmtSbt" :: "scalafmtAll" :: state)

  val FmtSbtCheckCommand =
    Command.command("check")(state => "scalafmtSbtCheck" :: "scalafmtCheckAll" :: state)

  val value = Seq(
    FmtSbtCommand,
    FmtSbtCheckCommand
  )

}
