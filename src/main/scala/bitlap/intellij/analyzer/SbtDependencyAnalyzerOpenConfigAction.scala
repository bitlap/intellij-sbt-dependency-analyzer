package bitlap.intellij.analyzer

import org.jetbrains.sbt.project.SbtProjectSystem

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerView
import com.intellij.openapi.externalSystem.dependency.analyzer.ExternalSystemDependencyAnalyzerOpenConfigAction
import bitlap.intellij.analyzer.SbtDependencyAnalyzerContributor.MODULE_DATA
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerDependency as Dependency

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/1
 */
final class SbtDependencyAnalyzerOpenConfigAction
    extends ExternalSystemDependencyAnalyzerOpenConfigAction(SbtProjectSystem.Id) {

  override def getExternalProjectPath(e: AnActionEvent): String = {
    val dependency = e.getData(DependencyAnalyzerView.DEPENDENCY)
    if (dependency == null) return null
    val dependencyData = dependency.data.asInstanceOf[Dependency.Data.Module]
    if (dependencyData == null) return null
    val moduleData = dependencyData.getUserData(MODULE_DATA)
    if (moduleData == null) return null
    moduleData.linkedExternalProjectPath
  }
}
