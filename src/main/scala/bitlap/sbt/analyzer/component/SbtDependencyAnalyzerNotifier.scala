package bitlap.sbt.analyzer.component

import java.nio.file.Path

import bitlap.sbt.analyzer.*
import bitlap.sbt.analyzer.task.SbtShellOutputAnalysisTask

import org.jetbrains.plugins.scala.*
import org.jetbrains.plugins.scala.project.Version

import com.intellij.notification.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.externalSystem.autoimport.ProjectRefreshAction
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil

/** SbtDependencyAnalyzer global notifier
 */
object SbtDependencyAnalyzerNotifier {

  private lazy val GROUP =
    NotificationGroupManager.getInstance().getNotificationGroup("Sbt.DependencyAnalyzer.Notification")

  private def getTextForAnalyzer(project: Project): String = {
    val sbtVersion = Version(SbtUtils.getSbtVersion(project))
    if (sbtVersion.major(2) >= Version("1.4")) {
      "addDependencyTreePlugin"
    } else {
      if (sbtVersion.major(3) >= Version("0.13.10")) {
        "addSbtPlugin(\"net.virtual-void\" % \"sbt-dependency-graph\" % \"0.9.2\")"
      } else {
        "addSbtPlugin(\"net.virtual-void\" % \"sbt-dependency-graph\" % \"0.8.2\")"
      }

    }
  }

  def addDependencyTreePlugin(project: Project): Unit = {
    // get project/plugins.sbt
    val projectPath    = VfsUtil.findFile(Path.of(project.getBasePath), true)
    val pluginsSbtFile = VfsUtil.findRelativeFile(projectPath, "project", "plugins.sbt")

    // add notification
    val notification = GROUP
      .createNotification(
        SbtDependencyAnalyzerBundle.message("sbt.dependency.analyzer.error.title"),
        SbtDependencyAnalyzerBundle.message("sbt.dependency.analyzer.error"),
        NotificationType.ERROR
      )
      .setIcon(SbtDependencyAnalyzerIcons.ICON)
    if (pluginsSbtFile != null) {
      notification.addAction(
        new NotificationAction(
          SbtDependencyAnalyzerBundle.message("sbt.dependency.analyzer.notification.goto.plugins.sbt")
        ) {
          override def actionPerformed(e: AnActionEvent, notification: Notification): Unit = {
            val doc = FileDocumentManager.getInstance().getDocument(pluginsSbtFile)
            WriteCommandAction.runWriteCommandAction(
              project,
              new Runnable() {
                override def run(): Unit = {
                  notification.expire()
                  doc.setReadOnly(false)
                  // modify plugins.sbt
                  doc.setText(doc.getText + Constants.Line_Separator + getTextForAnalyzer(project))
                  FileEditorManager
                    .getInstance(project)
                    .openTextEditor(new OpenFileDescriptor(project, pluginsSbtFile), true)
                  // if Intellij not enable auto-reload
                  ProjectRefreshAction.Companion.refreshProject(project)
                }
              }
            )
          }
        }
      )
    }
    notification.notify(project)
  }
}
