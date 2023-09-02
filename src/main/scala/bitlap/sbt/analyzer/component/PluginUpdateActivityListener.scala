package bitlap
package sbt
package analyzer
package component

import bitlap.sbt.analyzer.SbtDependencyAnalyzerBundle

import org.jetbrains.plugins.scala.project.Version

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.*
import com.intellij.notification.impl.NotificationsManagerImpl
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.wm.IdeFrame
import com.intellij.ui.BalloonImpl
import com.intellij.ui.BalloonLayoutData
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBUI

/** @author
 *    梦境迷离
 *  @version 1.0,2023/9/1
 */
object PluginUpdateActivityListener:
  private val Initial_Version              = "0.0.0"
  private val Update_Notification_Group_Id = "Sbt.DependencyAnalyzer.Notification"
  private val Version_Property             = s"${SbtDependencyAnalyzerPlugin.PLUGIN_ID}.version"

  private class UrlAction(version: Version)
      extends DumbAwareAction(
        SbtDependencyAnalyzerBundle.message("sbt.dependency.analyzer.updated.notification.goto"),
        null,
        AllIcons.General.Web
      ) {

    override def actionPerformed(e: AnActionEvent) = {

      BrowserUtil.browse(WhatsNew.getReleaseNotes(version))
    }
  }

end PluginUpdateActivityListener

final class PluginUpdateActivityListener extends BaseProjectActivity {

  import PluginUpdateActivityListener.*
  import WhatsNew.*

  override def onRunActivity(project: Project) = {
    checkUpdate(project)
  }

  private def checkUpdate(project: Project): Unit = {
    val plugin            = SbtDependencyAnalyzerPlugin.descriptor
    val versionString     = plugin.getVersion
    val properties        = PropertiesComponent.getInstance()
    val lastVersionString = properties.getValue(Version_Property, Initial_Version)
    if (versionString == lastVersionString) {
      return
    }

    val version     = Version(versionString)
    val lastVersion = Version(lastVersionString)
    if (version == lastVersion) {
      return
    }

    // Simple handling of notifications
    val isNewVersion = version > lastVersion
    if (isNewVersion && showUpdateNotification(project, plugin, version)) {
      properties.setValue(Version_Property, versionString)
    }
  }

  private def showUpdateNotification(
    project: Project,
    plugin: IdeaPluginDescriptor,
    version: Version
  ): Boolean = {
    val title = SbtDependencyAnalyzerBundle.message(
      "sbt.dependency.analyzer.updated.notification.title",
      plugin.getName,
      version.presentation
    )
    val partStyle = s"margin-top: ${JBUI.scale(8)}px;"
    val content = SbtDependencyAnalyzerBundle.message(
      "sbt.dependency.analyzer.updated.notification.message",
      partStyle,
      if (plugin.getChangeNotes == null) "<ul><li></li></ul>" else plugin.getChangeNotes,
      version.presentation
    )

    val notificationGroup = NotificationGroupManager.getInstance().getNotificationGroup(Update_Notification_Group_Id)
    if (notificationGroup == null) return false

    val notification = notificationGroup
      .createNotification(content, NotificationType.INFORMATION)
      .setTitle(title)
      .setImportant(true)
      .setIcon(SbtDependencyAnalyzerIcons.ICON)

    if (!canBrowseInHTMLEditor) {
      notification.addAction(new UrlAction(version))
    } else {
      notification.whenExpired(() => BrowserUtil.browse(WhatsNew.getReleaseNotes(version)))
    }

    notification.notify(project)

    true
  }
}
