package bitlap.sbt.analyzer

import javax.swing.JComponent

import com.intellij.openapi.options.SearchableConfigurable

/** @author
 *    梦境迷离
 *  @version 1.0,2023/9/7
 */
final class SbtDependencyAnalyzerConfigurable extends SearchableConfigurable {

  override def getId(): String = SbtDependencyAnalyzerPlugin.PLUGIN_ID

  override def getDisplayName(): String = SbtDependencyAnalyzerBundle.message("analyzer.settings.page.name")

  override def getHelpTopic(): String = "default"

  override def createComponent(): JComponent = ???

  override def getPreferredFocusedComponent(): JComponent = ???

  override def isModified(): Boolean = ???

  override def apply(): Unit = ???

  override def reset(): Unit = ???

  override def disposeUIResources(): Unit = ???

}
