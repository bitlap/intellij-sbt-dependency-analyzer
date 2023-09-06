package bitlap.sbt.analyzer.task

import bitlap.sbt.analyzer.util.SbtUtils

import org.jetbrains.plugins.scala.project.Version

import com.intellij.openapi.project.Project

/** Process the `set csrConfiguration;update` command, load fresh snapshots for analysis plugin.
 *
 *  @author
 *    梦境迷离
 *  @version 1.0,2023/8/19
 */
final class RefreshSnapshotsTask extends SbtShellOutputAnalysisTask[Unit]:

  override def executeCommand(project: Project): Unit =
    val sbtVersion = Version(SbtUtils.getSbtVersion(project))
    // see https://www.scala-sbt.org/1.x/docs/Dependency-Management-Flow.html#Notes+on+SNAPSHOTs
    if (sbtVersion.major(2) >= Version("1.3")) {
      getCommandOutputLines(
        project,
        """
          |set csrConfiguration := csrConfiguration.value.withTtl(Option(scala.concurrent.duration.DurationInt(0).seconds));
          |update
          |""".stripMargin
      )
    } else {
      getCommandOutputLines(
        project,
        """
          |update
          |""".stripMargin
      )
    }

end RefreshSnapshotsTask
