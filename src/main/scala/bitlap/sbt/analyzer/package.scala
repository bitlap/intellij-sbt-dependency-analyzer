package bitlap.sbt.analyzer

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters.*

import bitlap.sbt.analyzer.parsing.DependencyGraphType

import org.jetbrains.sbt.project.*

import com.intellij.buildsystem.model.unified.UnifiedCoordinates
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerDependency
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerDependency.Data
import com.intellij.openapi.externalSystem.model.project.*
import com.intellij.openapi.externalSystem.service.project.IdeModelsProviderImpl
import com.intellij.openapi.externalSystem.util.*
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key

lazy val Module_Data: Key[ModuleData] = Key.create[ModuleData]("SbtDependencyAnalyzerContributor.ModuleData")

given ExecutionContext = ExecutionContext.Implicits.global

given DependencyGraphType = DependencyGraphType.Dot

def getUnifiedCoordinates(dependency: DependencyAnalyzerDependency): UnifiedCoordinates =
  dependency.getData match {
    case data: DependencyAnalyzerDependency.Data.Artifact => getUnifiedCoordinates(data)
    case data: DependencyAnalyzerDependency.Data.Module   => getUnifiedCoordinates(data)
  }

def getUnifiedCoordinates(data: DependencyAnalyzerDependency.Data.Artifact): UnifiedCoordinates =
  UnifiedCoordinates(data.getGroupId, data.getArtifactId, data.getVersion)

def getUnifiedCoordinates(data: DependencyAnalyzerDependency.Data.Module): UnifiedCoordinates = {
  val moduleData = data.getUserData(Module_Data)
  if (moduleData == null) return null
  UnifiedCoordinates(moduleData.getGroup, moduleData.getExternalName, moduleData.getVersion)
}

def getParentModule(
  project: Project,
  dependency: DependencyAnalyzerDependency
): (DependencyAnalyzerDependency, Module) = {
  val parentData = dependency.getParent
  if (parentData == null) return getRootModule(project, dependency, dependency)
  dependency.getParent.getData match
    case _: Data.Module =>
      val data = dependency.getParent.getData.asInstanceOf[DependencyAnalyzerDependency.Data.Module]
      dependency -> getModule(project, data)
    case _ => getRootModule(project, dependency, dependency)
}

def getRootModule(
  project: Project,
  dependency: DependencyAnalyzerDependency,
  parent: DependencyAnalyzerDependency
): (DependencyAnalyzerDependency, Module) = {
  parent.getData match
    case _: Data.Module =>
      val data = parent.getData.asInstanceOf[DependencyAnalyzerDependency.Data.Module]
      dependency -> getModule(project, data)
    case _ => getRootModule(project, parent, parent.getParent)
}

def getModule(project: Project, data: DependencyAnalyzerDependency.Data.Module): Module = {
  val moduleData: ModuleData = data.getUserData(Module_Data)
  if (moduleData == null) return null
  findModule(project, moduleData)
}

def findDependsModules(module: Module): List[Module] = {
  val modelsProvider = new IdeModelsProviderImpl(module.getProject)
  modelsProvider.getAllDependentModules(module).asScala.toList
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

def waitInterval(sleep: Duration = Constants.INTERVAL_TIMEOUT): Unit = {
  try {
    Thread.sleep(sleep.toMillis)
  } catch {
    case _: Throwable =>
  }
}
