package bitlap
package sbt
package analyzer
package util

import java.nio.file.Path

import scala.concurrent.duration.*

import bitlap.sbt.analyzer.activity.WhatsNew
import bitlap.sbt.analyzer.activity.WhatsNew.canBrowseInHTMLEditor

import org.jetbrains.plugins.scala.*
import org.jetbrains.plugins.scala.extensions.*
import org.jetbrains.plugins.scala.project.Version

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.notification.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.{ DumbAwareAction, Project }
import com.intellij.openapi.vfs.{ VfsUtil, VirtualFile }

/** SbtDependencyAnalyzer global notifier
 */
object Notifications {

  private lazy val NotificationGroup =
    NotificationGroupManager.getInstance().getNotificationGroup("Sbt.DependencyAnalyzer.Notification")

  private def getSdapText(project: Project): String = {
    val sbtVersion = Version(SbtUtils.getSbtVersion(project))
    val line = if (sbtVersion.major(2) >= Version("1.4")) {
      "addDependencyTreePlugin"
    } else {
      if (sbtVersion.major(3) >= Version("0.13.10")) {
        "addSbtPlugin(\"net.virtual-void\" % \"sbt-dependency-graph\" % \"0.9.2\")"
      } else {
        "addSbtPlugin(\"net.virtual-void\" % \"sbt-dependency-graph\" % \"0.8.2\")"
      }
    }
    "// -- This file was mechanically generated by Sbt Dependency Analyzer Plugin: Do not edit! -- //" + Constants.LINE_SEPARATOR
      + line + Constants.LINE_SEPARATOR
  }

  def notifyParseFileError(file: String, msg: String): Unit = {
    // add notification when get vfsFile timeout
    val notification = NotificationGroup
      .createNotification(
        SbtDependencyAnalyzerBundle.message("analyzer.task.error.title"),
        SbtDependencyAnalyzerBundle.message("analyzer.task.error.text", file, msg),
        NotificationType.ERROR
      )
      .setIcon(SbtDependencyAnalyzerIcons.ICON)
      .setImportant(true)
    notification.notify(null)
  }

  def notifySettingsChanged(project: Project): Unit = {
    val notification = NotificationGroup
      .createNotification(
        SbtDependencyAnalyzerBundle.message("analyzer.notification.setting.changed.title"),
        NotificationType.INFORMATION
      )
      .setIcon(SbtDependencyAnalyzerIcons.ICON)
    notification.notify(project)
  }

  def notifyDependencyChanged(
    project: Project,
    dependency: String,
    success: Boolean = true,
    self: Boolean = false
  ): Unit = {
    val msg =
      if (!self) {
        if (success) SbtDependencyAnalyzerBundle.message("analyzer.notification.dependency.excluded.title", dependency)
        else SbtDependencyAnalyzerBundle.message("analyzer.notification.dependency.excluded.failed.title", dependency)
      } else {
        if (success)
          SbtDependencyAnalyzerBundle.message("analyzer.notification.dependency.removed.title", dependency)
        else SbtDependencyAnalyzerBundle.message("analyzer.notification.dependency.removed.failed.title", dependency)
      }
    NotificationGroup
      .createNotification(msg, NotificationType.INFORMATION)
      .setIcon(SbtDependencyAnalyzerIcons.ICON)
      .addAction(
        new NotificationAction(
          SbtDependencyAnalyzerBundle.message("analyzer.notification.ok")
        ) {
          override def actionPerformed(e: AnActionEvent, notification: Notification): Unit = {
            inReadAction {
              notification.expire()
            }

          }
        }
      )
      .notify(project)
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
      .setImportant(true)
    notification.notify(project)
  }

  def notifyAndCreateSdapFile(project: Project): Unit = {
    // get project/plugins.sbt
    // val pluginSbtFileName = "plugins.sbt"
    val pluginSbtFileName   = "sdap.sbt"
    val basePath            = VfsUtil.findFile(Path.of(project.getBasePath), true)
    implicit val p: Project = project
    // 1. get or create sdap.sbt file and add dependency tree statement
    inWriteCommandAction {
      val sdapText    = getSdapText(project)
      val projectPath = VfsUtil.createDirectoryIfMissing(basePath, "project")

      var pluginsSbtFile     = projectPath.findChild(pluginSbtFileName)
      val isSdapAutoGenerate = pluginsSbtFile.isSdapAutoGenerate(sdapText)
      if (isSdapAutoGenerate) {
        // add to git ignore
        val gitExclude    = VfsUtil.findRelativeFile(basePath, ".git", "info", "exclude")
        val gitExcludeDoc = gitExclude.document()
        if (gitExcludeDoc != null) {
          val ignoreText = "project" + Constants.SEPARATOR + pluginSbtFileName
          if (gitExcludeDoc.getText != null && !gitExcludeDoc.getText.contains(ignoreText)) {
            gitExcludeDoc.setReadOnly(false)
            gitExcludeDoc.setText(
              gitExcludeDoc.getText + Constants.LINE_SEPARATOR + ignoreText + Constants.LINE_SEPARATOR
            )
          }
        }
        pluginsSbtFile = projectPath.findOrCreateChildData(null, pluginSbtFileName)
      }

      val doc = pluginsSbtFile.document()
      doc.setReadOnly(false)
      if (isSdapAutoGenerate) {
        doc.setText(sdapText)
      } else {
        doc.setText(doc.getText + Constants.LINE_SEPARATOR + sdapText)
      }
      // if intellij not enable auto-reload
      // force refresh project
      // SbtUtils.refreshProject(project)
      // SbtUtils.untilProjectReady(project)

    }
    invokeAndWait(SbtUtils.forceRefreshProject(project))
    // 2. add notification
    NotificationGroup
      .createNotification(
        SbtDependencyAnalyzerBundle.message("analyzer.notification.addSdap.title"),
        SbtDependencyAnalyzerBundle.message("analyzer.notification.addSdap.text", pluginSbtFileName),
        NotificationType.INFORMATION
      )
      .setImportant(true)
      .setIcon(SbtDependencyAnalyzerIcons.ICON)
      .addAction(
        new NotificationAction(
          SbtDependencyAnalyzerBundle.message("analyzer.notification.gotoSdap", pluginSbtFileName)
        ) {
          override def actionPerformed(e: AnActionEvent, notification: Notification): Unit = {
            inReadAction {
              notification.expire()
              val recheckFile = VfsUtil.findRelativeFile(basePath, "project", pluginSbtFileName)
              if (recheckFile != null) {
                FileEditorManager
                  .getInstance(project)
                  .openTextEditor(new OpenFileDescriptor(project, recheckFile), true)
              }
            }

          }
        }
      )
      .notify(project)

  }

  /** notify information when update plugin
   */
  def notifyUpdateActivity(project: Project, version: Version, title: String, content: String): Unit = {
    val notification = NotificationGroup
      .createNotification(content, NotificationType.INFORMATION)
      .setTitle(title)
      .setImportant(true)
      .setIcon(SbtDependencyAnalyzerIcons.ICON)
      .setListenerIfSupport(NotificationListener.URL_OPENING_LISTENER)
    if (canBrowseInHTMLEditor) {
      notification.whenExpired(() => WhatsNew.browse(version, project))
    } else {
      notification.addAction(
        new DumbAwareAction(
          SbtDependencyAnalyzerBundle.message("analyzer.notification.updated.gotoBrowser"),
          null,
          AllIcons.General.Web
        ) {
          override def actionPerformed(e: AnActionEvent): Unit =
            notification.expire()
            BrowserUtil.browse(WhatsNew.getReleaseNotes(version))
        }
      )
    }
    notification.notify(project)
    if (canBrowseInHTMLEditor && SbtUtils.untilProjectReady(project)) {
      waitInterval(10.seconds)
      notification.expire()
    }
  }

  extension (notification: Notification) {

    private def setListenerIfSupport(listener: NotificationListener): Notification = {
      try {
        org.joor.Reflect.on(notification).call("setListener", listener)
      } catch {
        case _: Throwable =>
        // ignore
      }
      notification
    }
  }

  extension (file: VirtualFile) {

    private def document(): Document = {
      if (file == null) {
        return null
      }
      val doc = FileDocumentManager.getInstance().getDocument(file)
      doc
    }

    private def isSdapAutoGenerate(sdapText: String): Boolean = {
      if (file == null) {
        return true
      }
      val doc = FileDocumentManager.getInstance().getDocument(file)
      doc == null || doc.getText == null || doc.getText.trim.isEmpty || doc.getText.trim == sdapText
    }
  }
}
