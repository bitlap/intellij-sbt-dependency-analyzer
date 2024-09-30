package bitlap
package sbt
package analyzer
package action

import bitlap.sbt.analyzer.util.SbtUtils

import com.intellij.openapi.actionSystem.AnActionEvent

final class SbtRefreshDependenciesAction extends BaseRefreshDependenciesAction:

  override lazy val eventText: String = SbtDependencyAnalyzerBundle.message("analyzer.refresh.dependencies.text")

  override lazy val eventDescription: String =
    SbtDependencyAnalyzerBundle.message("analyzer.refresh.dependencies.description")

  override def actionPerformed(e: AnActionEvent): Unit = {
    SbtDependencyAnalyzerContributor.isAvailable.set(false)
    SbtUtils.refreshProject(e.getProject)
    SbtDependencyAnalyzerContributor.isAvailable.set(true)
  }

end SbtRefreshDependenciesAction
