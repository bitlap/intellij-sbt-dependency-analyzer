package bitlap
package sbt
package analyzer

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Transient

import kotlin.jvm.Volatile

/** @author
 *    梦境迷离
 *  @version 1.0,2023/9/7
 */
@State(name = "SbtDependencyAnalyzer.Settings", storages = Array(new Storage("bitlap.sbt.dependency.analyzer.xml")))
final class Settings extends PersistentStateComponent[Settings] {

  import Settings.*

  @Volatile
  @Transient
  private var isInitialized = false

  private var _languageSelection: AnalyzerLanguage = AnalyzerLanguage.DEFAULT
  private var _analyzedScope: String               = DependencyScopeEnum.values.map(_.toString).mkString(",")
  private var _ignoredModules: String              = _
  private var _organization: String                = _

  def languageSelection: AnalyzerLanguage = _languageSelection

  def languageSelection_=(languageSelection: AnalyzerLanguage): Unit = {
    if (isInitialized && _languageSelection != languageSelection) {
      SETTINGS_CHANGE_PUBLISHER.onAnalyzerConfigurationChanged()
    }
    _languageSelection = languageSelection
  }

  def analyzedScope: String = _analyzedScope

  def analyzedScope_=(analyzedScope: String): Unit = {
    if (isInitialized && _analyzedScope != analyzedScope) {
      SETTINGS_CHANGE_PUBLISHER.onAnalyzerConfigurationChanged()
    }
    _analyzedScope = analyzedScope
  }

  def ignoredModules: String = _ignoredModules

  def ignoredModules_=(ignoredModules: String): Unit = {
    if (isInitialized && _ignoredModules != ignoredModules) {
      SETTINGS_CHANGE_PUBLISHER.onAnalyzerConfigurationChanged()
    }
    _ignoredModules = ignoredModules
  }

  def organization: String = _organization

  def organization_=(organization: String): Unit = {
    if (isInitialized && _organization != organization) {
      SETTINGS_CHANGE_PUBLISHER.onAnalyzerConfigurationChanged()
    }
    _organization = organization
  }

  override def getState(): Settings = this

  override def loadState(state: Settings): Unit = {
    XmlSerializerUtil.copyBean(state, this)
    val properties: PropertiesComponent = PropertiesComponent.getInstance()
    val dataVersion                     = properties.getInt(DataVersionKey, 0)
    if (dataVersion < CurrentDataVersion) {
      properties.setValue(DataVersionKey, CurrentDataVersion, 0)
    }
  }

  override def initializeComponent(): Unit = {
    isInitialized = true
  }
}

object Settings {

  private[analyzer] enum AnalyzerLanguage(val displayName: String) {

    case DEFAULT extends AnalyzerLanguage(SbtDependencyAnalyzerBundle.message("analyzer.settings.item.main.or.english"))

    case PRIMARY_LANGUAGE
        extends AnalyzerLanguage(SbtDependencyAnalyzerBundle.message("analyzer.settings.item.primaryLanguage"))
  }

  object SettingsChangeListener:

    val _Topic: Topic[SettingsChangeListener] =
      Topic.create("SbtDependencyAnalyzerSettingsChanged", classOf[SettingsChangeListener])
  end SettingsChangeListener

  trait SettingsChangeListener:

    def onAnalyzerConfigurationChanged(): Unit

  end SettingsChangeListener

  private val CurrentDataVersion = 1
  private val DataVersionKey     = s"${SbtDependencyAnalyzerPlugin.PLUGIN_ID}.settings.data.version"

  private val SETTINGS_CHANGE_PUBLISHER: SettingsChangeListener =
    ApplicationManager.getApplication.getMessageBus.syncPublisher(SettingsChangeListener._Topic)

}
