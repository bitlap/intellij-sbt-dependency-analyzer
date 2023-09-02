package bitlap
package sbt
package analyzer
package action

import org.jetbrains.sbt.project.SbtProjectSystem

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.dependency.analyzer.{
  DependencyAnalyzerDependency as Dependency,
  DependencyAnalyzerView,
  ExternalSystemDependencyAnalyzerOpenConfigAction
}

final class SbtDependencyAnalyzerOpenConfigAction
    extends ExternalSystemDependencyAnalyzerOpenConfigAction(SbtProjectSystem.Id):

  override def getExternalProjectPath(e: AnActionEvent): String =
    val dependency = e.getData(DependencyAnalyzerView.Companion.getDEPENDENCY)
    if (dependency == null) return null
    dependency.getData match
      case dm: Dependency.Data.Module =>
        val moduleData = dm.getUserData(Module_Data)
        if (moduleData == null) null else moduleData.getLinkedExternalProjectPath
      case _ => null
  end getExternalProjectPath

end SbtDependencyAnalyzerOpenConfigAction
