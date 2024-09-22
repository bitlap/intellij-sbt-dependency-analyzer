package bitlap
package sbt
package analyzer
package action

import bitlap.sbt.analyzer.util.SbtUtils

import org.jetbrains.sbt.project.SbtProjectSystem

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.dependency.analyzer.{
  DependencyAnalyzerDependency as Dependency,
  DependencyAnalyzerView,
  ExternalSystemDependencyAnalyzerOpenConfigAction
}
import com.intellij.openapi.externalSystem.service.settings.ExternalSystemConfigLocator
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile

final class SbtDependencyAnalyzerOpenConfigAction
    extends ExternalSystemDependencyAnalyzerOpenConfigAction(SbtProjectSystem.Id):

  override def getConfigFile(e: AnActionEvent): VirtualFile = {
    val externalSystemConfigPath = getConfigFileOption(Option(getExternalProjectPath(e)))
    // if we cannot find module config, goto root config
    val configPath = externalSystemConfigPath
      .orElse(getConfigFileOption(SbtUtils.getExternalProjectPath(e.getProject).headOption))
      .orNull
    if (configPath == null || configPath.isDirectory) null else configPath
  }

  private def getConfigFileOption(externalProjectPath: Option[String]): Option[VirtualFile] = {
    val fileSystem               = LocalFileSystem.getInstance()
    val externalProjectDirectory = externalProjectPath.map(fileSystem.refreshAndFindFileByPath)
    val locator = ExternalSystemConfigLocator.EP_NAME.findFirstSafe(_.getTargetExternalSystemId == SbtProjectSystem.Id)
    if (locator == null) {
      return null
    }

    val externalSystemConfigPath = externalProjectDirectory.toList.filterNot(_ == null).map(locator.adjust)

    externalSystemConfigPath.filterNot(_ == null).headOption
  }

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
