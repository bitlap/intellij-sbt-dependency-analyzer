package bitlap.sbt.analyzer.util

import java.nio.file.Path

import bitlap.sbt.analyzer.*

import org.jetbrains.plugins.scala.*
import org.jetbrains.plugins.scala.project.Version
import org.jetbrains.sbt.project.SbtProjectSystem

import com.intellij.notification.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.externalSystem.autoimport.ProjectRefreshAction
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil

/** SbtDependencyAnalyzer global notifier
 */
object Notifications {

  private lazy val NotificationGroup =
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

  def notifyParseFileError(file: String): Unit = {
    // add notification when gets vfsFile timeout
    val notification = NotificationGroup
      .createNotification(
        SbtDependencyAnalyzerBundle.message("analyzer.task.error.title"),
        SbtDependencyAnalyzerBundle.message("analyzer.task.error.text", file),
        NotificationType.ERROR
      )
      .setIcon(SbtDependencyAnalyzerIcons.ICON)
    notification.notify(null)
  }

  def notifyUnknownError(project: Project, command: String, moduleId: String, scope: DependencyScopeEnum): Unit = {
    // add notification
    val notification = NotificationGroup
      .createNotification(
        SbtDependencyAnalyzerBundle.message("analyzer.task.error.title"),
        SbtDependencyAnalyzerBundle.message("analyzer.task.error.unknown.text", moduleId, scope.toString, command),
        NotificationType.ERROR
      )
      .setIcon(SbtDependencyAnalyzerIcons.ICON)
    notification.notify(project)
  }

  def notifyAndAddDependencyTreePlugin(project: Project): Unit = {
    // get project/plugins.sbt
    val projectPath    = VfsUtil.findFile(Path.of(project.getBasePath), true)
    val pluginsSbtFile = VfsUtil.findRelativeFile(projectPath, "project", "plugins.sbt")

    // add notification
    val notification = NotificationGroup
      .createNotification(
        SbtDependencyAnalyzerBundle.message("analyzer.task.error.title"),
        SbtDependencyAnalyzerBundle.message("analyzer.task.error.unknown.title"),
        NotificationType.ERROR
      )
      .setIcon(SbtDependencyAnalyzerIcons.ICON)
      .setImportant(true)
    if (pluginsSbtFile != null) {
      notification.addAction(
        new NotificationAction(
          SbtDependencyAnalyzerBundle.message("analyzer.notification.gotoPluginsFile")
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
                }
              }
            )
            // force refresh project
            ExternalSystemUtil.refreshProjects(new ImportSpecBuilder(project, SbtProjectSystem.Id))
          }
        }
      )
    }
    notification.notify(project)
  }
}
