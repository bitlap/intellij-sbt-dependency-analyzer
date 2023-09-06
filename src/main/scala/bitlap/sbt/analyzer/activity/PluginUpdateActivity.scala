package bitlap
package sbt
package analyzer
package activity

import bitlap.sbt.analyzer.*
import bitlap.sbt.analyzer.util.Notifications

import org.jetbrains.plugins.scala.project.Version

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.*
import com.intellij.openapi.util.*
import com.intellij.ui.*
import com.intellij.util.ui.JBUI

/** @author
 *    梦境迷离
 *  @version 1.0,2023/9/1
 */
object PluginUpdateActivity:
  private val InitialVersion       = "0.0.0"
  private lazy val VersionProperty = s"${SbtDependencyAnalyzerPlugin.PLUGIN_ID}.version"

end PluginUpdateActivity

final class PluginUpdateActivity extends BaseProjectActivity {

  import PluginUpdateActivity.*

  override def onRunActivity(project: Project) = {
    checkUpdate(project)
  }

  private def checkUpdate(project: Project): Unit = {
    val plugin            = SbtDependencyAnalyzerPlugin.descriptor
    val versionString     = plugin.getVersion
    val properties        = PropertiesComponent.getInstance()
    val lastVersionString = properties.getValue(VersionProperty, InitialVersion)
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
      properties.setValue(VersionProperty, versionString)
    }
  }

  private def showUpdateNotification(
    project: Project,
    plugin: IdeaPluginDescriptor,
    version: Version
  ): Boolean = {
    val latestChangeNotes =
      if (plugin.getChangeNotes == null) "<ul><li></li></ul>"
      else plugin.getChangeNotes.split(Constants.ChangeNotesSeparator)(0)
    val title = SbtDependencyAnalyzerBundle.message(
      "analyzer.updated.notification.title",
      plugin.getName,
      version.presentation
    )
    val partStyle = s"margin-top: ${JBUI.scale(8)}px;"
    val content = SbtDependencyAnalyzerBundle.message(
      "analyzer.updated.notification.text",
      partStyle,
      latestChangeNotes,
      version.presentation
    )
    Notifications.notifyUpdateActivity(project, version, title, content)
    true
  }
}
