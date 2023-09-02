package bitlap.sbt.analyzer.component

import bitlap.sbt.analyzer.SbtDependencyAnalyzerBundle
import bitlap.sbt.analyzer.SbtDependencyAnalyzerPlugin

import org.jetbrains.plugins.scala.project.Version

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

final class WhatsNewAction extends DumbAwareAction {

  getTemplatePresentation.setText(
    SbtDependencyAnalyzerBundle.message("sbt.dependency.analyzer.action.WhatsNew.text", "Sbt Dependency Analyzer")
  )

  override def actionPerformed(e: AnActionEvent): Unit = {
    WhatsNew.browse(Version(SbtDependencyAnalyzerPlugin.descriptor.getVersion), e.getProject)
  }
}
