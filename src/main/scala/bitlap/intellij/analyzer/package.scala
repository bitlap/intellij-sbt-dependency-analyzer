package bitlap.intellij.analyzer

import java.util.Collections
import java.util.concurrent.atomic.AtomicLong

import org.jetbrains.sbt.SbtUtil
import org.jetbrains.sbt.project.{ SbtProjectSystem, SbtTaskManager }

import com.intellij.build.SyncViewManager
import com.intellij.buildsystem.model.unified.UnifiedCoordinates
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerDependency
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.model.project.{ ModuleData, ProjectData }
import com.intellij.openapi.externalSystem.service.execution.*
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunnableState.*
import com.intellij.openapi.externalSystem.service.project.IdeModelsProviderImpl
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.{ ExternalSystemApiUtil, ExternalSystemUtil }
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase

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
  val moduleData = data.getUserData(SbtDependencyAnalyzerContributor.Module_Data)
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
  val moduleData: ModuleData = data.getUserData(SbtDependencyAnalyzerContributor.Module_Data)
  if (moduleData == null) return null
  findModule(project, moduleData)
}

def findModule(project: Project, moduleData: ModuleData): Module = {
  val modelsProvider = new IdeModelsProviderImpl(project)
  modelsProvider.findIdeModule(moduleData)
}

def findModule(project: Project, projectData: ProjectData): Module =
  findModule(project, projectData.getLinkedExternalProjectPath)

def findModule(project: Project, projectPath: String): Module = {
  val moduleNode = ExternalSystemApiUtil.findModuleNode(project, SbtProjectSystem.Id, projectPath)
  if (moduleNode == null) return null
  findModule(project, moduleNode.getData)
}

val id = new AtomicLong(0)
