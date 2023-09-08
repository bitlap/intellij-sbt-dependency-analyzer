package bitlap.sbt.analyzer

import javax.swing.JComponent

import com.intellij.openapi.options.*
import com.intellij.openapi.options.Configurable.Composite

/** @author
 *    梦境迷离
 *  @version 1.0,2023/9/7
 */
final class SbtDependencyAnalyzerConfigurable extends SearchableConfigurable {

  // create a ui form
  private val panel: SbtDependencyAnalyzerPanel = new SbtDependencyAnalyzerPanel(SettingsState.instance)

  override def getId(): String = SbtDependencyAnalyzerPlugin.PLUGIN_ID

  override def getDisplayName(): String = SbtDependencyAnalyzerBundle.message("analyzer.settings.page.name")

  override def getHelpTopic(): String = "default"

  override def createComponent(): JComponent = panel.$$$getRootComponent$$$()

  override def isModified(): Boolean = panel.isModified

  override def apply(): Unit = panel.apply()

  override def reset(): Unit = panel.from()

  override def disposeUIResources(): Unit = {}

}
