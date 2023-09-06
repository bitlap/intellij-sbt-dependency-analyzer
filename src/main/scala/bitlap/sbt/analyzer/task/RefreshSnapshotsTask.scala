package bitlap.sbt.analyzer.task

import com.intellij.openapi.project.Project

/** Process the `set csrConfiguration;update` command, load fresh snapshots for analysis plugin.
 *
 *  @author
 *    梦境迷离
 *  @version 1.0,2023/8/19
 */
final class RefreshSnapshotsTask extends SbtShellOutputAnalysisTask[Unit]:

  override def executeCommand(project: Project): Unit =
    getCommandOutputLines(
      project,
      """
        |set csrConfiguration := csrConfiguration.value.withTtl(Option(scala.concurrent.duration.DurationInt(0).seconds));
        |update
        |""".stripMargin
    )

end RefreshSnapshotsTask
