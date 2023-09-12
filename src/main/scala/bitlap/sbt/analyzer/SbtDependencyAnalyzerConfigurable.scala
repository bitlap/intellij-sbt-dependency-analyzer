package bitlap.sbt.analyzer

import javax.swing.JComponent

import com.intellij.openapi.options.*
import com.intellij.openapi.options.Configurable.Composite
import com.intellij.openapi.project.Project

/** @author
 *    梦境迷离
 *  @version 1.0,2023/9/7
 */
final class SbtDependencyAnalyzerConfigurable(project: Project) extends SearchableConfigurable {

  // create a ui form
  private val panel: SbtDependencyAnalyzerPanel = new SbtDependencyAnalyzerPanel(project)

  override def getId(): String = SbtDependencyAnalyzerPlugin.PLUGIN_ID

  override def getDisplayName(): String = SbtDependencyAnalyzerBundle.message("analyzer.settings.page.name")

  override def getHelpTopic(): String = "default"

  override def createComponent(): JComponent = panel.$$$getRootComponent$$$()

  override def isModified(): Boolean = panel.isModified

  override def apply(): Unit = panel.apply()

  override def reset(): Unit = panel.from()

  override def disposeUIResources(): Unit = {}

}
