package bitlap.sbt.analyzer

import javax.swing.JComponent

import com.intellij.openapi.options.*
import com.intellij.openapi.project.Project

final class SbtDependencyAnalyzerConfigurable(project: Project) extends SearchableConfigurable.Parent.Abstract {

  private lazy val panel: SbtDependencyAnalyzerPanel = new SbtDependencyAnalyzerPanel(project)

  override def getId: String = SbtDependencyAnalyzerPlugin.PLUGIN_ID

  override def getDisplayName: String = SbtDependencyAnalyzerBundle.message("analyzer.settings.page.name")

  override def getHelpTopic: String = "default"

  override def createComponent(): JComponent = panel.$$$getRootComponent$$$()

  override def isModified: Boolean = panel.isModified

  override def apply(): Unit = panel.apply()

  override def reset(): Unit = panel.from()

  override def disposeUIResources(): Unit = {}

  override def buildConfigurables(): Array[Configurable] = Array()
}
