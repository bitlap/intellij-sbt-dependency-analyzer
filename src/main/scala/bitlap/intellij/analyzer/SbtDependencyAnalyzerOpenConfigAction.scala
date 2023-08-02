package bitlap.intellij.analyzer

import bitlap.intellij.analyzer.SbtDependencyAnalyzerContributor.Module_Data

import org.jetbrains.sbt.project.SbtProjectSystem

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerDependency as Dependency
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerView
import com.intellij.openapi.externalSystem.dependency.analyzer.ExternalSystemDependencyAnalyzerOpenConfigAction

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/1
 */
final class SbtDependencyAnalyzerOpenConfigAction
    extends ExternalSystemDependencyAnalyzerOpenConfigAction(SbtProjectSystem.Id) {

  override def getExternalProjectPath(e: AnActionEvent): String = {
    val dependency = e.getData(DependencyAnalyzerView.Companion.getDEPENDENCY)
    if (dependency == null) return null
    val dependencyData = dependency.getData.asInstanceOf[Dependency.Data.Module]
    if (dependencyData == null) return null
    val moduleData = dependencyData.getUserData(Module_Data)
    if (moduleData == null) return null
    moduleData.getLinkedExternalProjectPath
  }
}
