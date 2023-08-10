package bitlap.sbt.analyzer

import java.util.concurrent.atomic.AtomicLong

import bitlap.sbt.analyzer.model.ModuleContext

import org.jetbrains.sbt.project.*

import com.intellij.buildsystem.model.unified.UnifiedCoordinates
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerDependency
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerDependency.Data
import com.intellij.openapi.externalSystem.model.project.*
import com.intellij.openapi.externalSystem.service.execution.*
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunnableState.*
import com.intellij.openapi.externalSystem.service.project.IdeModelsProviderImpl
import com.intellij.openapi.externalSystem.util.*
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
  val moduleData = data.getUserData(SbtDependencyAnalyzerContributor.Module_Data)
  if (moduleData == null) return null
  UnifiedCoordinates(moduleData.getGroup, moduleData.getExternalName, moduleData.getVersion)
}

def getParentModule(project: Project, dependency: DependencyAnalyzerDependency): Module = {
  val parentData = dependency.getParent
  if (parentData == null) return null
  dependency.getParent.getData match
    case module: Data.Module =>
      val data = dependency.getParent.getData.asInstanceOf[DependencyAnalyzerDependency.Data.Module]
      getModule(project, data)
    case _ => null
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

private val `scalaVerRegex`        = "(.*)(_)(.*)".r
private val `scalaJs0.6VerRegex`   = "(.*)(_sjs0\\.6_)(.*)".r
private val `scalaJs1VerRegex`     = "(.*)(_sjs1_)(.*)".r
private val `scalaNative1VerRegex` = "(.*)(_native0\\.4_)(.*)".r

final case class PlatformModule(
  module: String,
  platform: String,
  scalaVersion: String
)
