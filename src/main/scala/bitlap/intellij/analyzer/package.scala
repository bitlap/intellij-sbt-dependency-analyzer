package bitlap.intellij.analyzer

import org.jetbrains.plugins.gradle.util.GradleUtil
import org.jetbrains.sbt.SbtUtil

import com.intellij.buildsystem.model.unified.UnifiedCoordinates
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerDependency
import com.intellij.openapi.externalSystem.service.project.IdeModelsProviderImpl
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/1
 */

def getUnifiedCoordinates(dependency: DependencyAnalyzerDependency): UnifiedCoordinates =
  dependency.getData match {
    case data: DependencyAnalyzerDependency.Data.Artifact => getUnifiedCoordinates(data)
    case data: DependencyAnalyzerDependency.Data.Module   => getUnifiedCoordinates(data)
  }

def getUnifiedCoordinates(data: DependencyAnalyzerDependency.Data.Artifact): UnifiedCoordinates =
  UnifiedCoordinates(data.getGroupId, data.getArtifactId, data.getVersion)

def getUnifiedCoordinates(data: DependencyAnalyzerDependency.Data.Module): UnifiedCoordinates = {
  val moduleData = data.getUserData(SbtDependencyAnalyzerContributor.MODULE_DATA)
  if (moduleData == null) return null
  UnifiedCoordinates(moduleData.getGroup, moduleData.getExternalName, moduleData.getVersion)
}

def getParentModule(project: Project, dependency: DependencyAnalyzerDependency): Module = {
  val parentData = dependency.getParent
  if (parentData == null) return null
  val data = dependency.getParent.getData.asInstanceOf[DependencyAnalyzerDependency.Data.Module]
  getModule(project, data)
}

def getModule(project: Project, data: DependencyAnalyzerDependency.Data.Module): Module = {
  val moduleData = data.getUserData(SbtDependencyAnalyzerContributor.MODULE_DATA)
  if (moduleData == null) return null
  val modelsProvider = new IdeModelsProviderImpl(project)
  modelsProvider.findIdeModule(moduleData)
}
