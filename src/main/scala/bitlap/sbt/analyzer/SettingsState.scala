package bitlap
package sbt
package analyzer

import scala.beans.BeanProperty

import org.jetbrains.sbt.project.settings.*

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.openapi.externalSystem.settings.*
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Transient

import kotlin.jvm.Volatile

/** @author
 *    梦境迷离
 *  @version 1.0,2023/9/7
 */
@State(name = "SbtDependencyAnalyzer.Settings", storages = Array(new Storage("bitlap.sbt.dependency.analyzer.xml")))
@Service(Array(Service.Level.PROJECT))
final class SettingsState extends PersistentStateComponent[SettingsState] {

  import SettingsState.*

  @Volatile
  @Transient
  private var isInitialized = false

  @BeanProperty
  private var _languageSelection: AnalyzerLanguage = AnalyzerLanguage.DEFAULT

  @BeanProperty
  private var _analyzedScope: String = DependencyScopeEnum.values.map(_.toString).mkString(",")

  @BeanProperty
  private var _ignoredModules: String = _

  @BeanProperty
  private var _organization: String = _

  override def getState(): SettingsState = this

  override def loadState(state: SettingsState): Unit = {
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

object SettingsState {

  val instance: SettingsState = ApplicationManager.getApplication.getService(classOf[SettingsState])

  val _Topic: Topic[SettingsChangeListener] =
    Topic.create("SbtDependencyAnalyzerSettingsChanged", classOf[SettingsChangeListener])

  private[analyzer] enum AnalyzerLanguage(val displayName: String) {

    case DEFAULT extends AnalyzerLanguage(SbtDependencyAnalyzerBundle.message("analyzer.settings.item.main.or.english"))

    case PRIMARY_LANGUAGE
        extends AnalyzerLanguage(SbtDependencyAnalyzerBundle.message("analyzer.settings.item.primaryLanguage"))
  }

  trait SettingsChangeListener:

    def onAnalyzerConfigurationChanged(): Unit

  end SettingsChangeListener

  val CurrentDataVersion = 1
  val DataVersionKey     = s"${SbtDependencyAnalyzerPlugin.PLUGIN_ID}.settings.data.version"

  val SETTINGS_CHANGE_PUBLISHER: SettingsChangeListener =
    ApplicationManager.getApplication.getMessageBus.syncPublisher(_Topic)

}
