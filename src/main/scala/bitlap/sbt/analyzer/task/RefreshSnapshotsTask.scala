package bitlap
package sbt
package analyzer
package task

import org.jetbrains.plugins.scala.project.Version

import com.intellij.openapi.project.Project

import util.SbtUtils

/** Process the `set csrConfiguration;update` command, load fresh snapshots for sbt shell.
 */
final class RefreshSnapshotsTask extends SbtShellOutputAnalysisTask[Unit]:

  override def executeCommand(project: Project): Unit =
    val sbtVersion = Version(SbtUtils.getSbtVersion(project))
    // see https://www.scala-sbt.org/1.x/docs/Dependency-Management-Flow.html#Notes+on+SNAPSHOTs
    if (sbtVersion.major(2) >= Version("1.3")) {
      getCommandOutputLines(
        project,
        """
          |set update / skip := false;
          |set csrConfiguration := csrConfiguration.value.withTtl(Option(scala.concurrent.duration.DurationInt(0).seconds));
          |update;
          |""".stripMargin
      )
    } else {
      getCommandOutputLines(
        project,
        """
          |set update / skip := false;
          |update;
          |""".stripMargin
      )
    }

end RefreshSnapshotsTask
