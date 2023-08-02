package bitlap.intellij.analyzer.configuration

import java.util.function.Supplier
import javax.swing.Icon

import bitlap.intellij.analyzer.SbtPluginBundle
import bitlap.intellij.analyzer.configuration.SbtRuntimeType.TYPE_ID

import org.jetbrains.sbt.icons.Icons

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.target.LanguageRuntimeType
import com.intellij.execution.target.TargetEnvironmentConfiguration
import com.intellij.execution.target.TargetEnvironmentType
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/2
 */
final class SbtRuntimeType extends LanguageRuntimeType[SbtRuntimeTargetConfiguration](TYPE_ID) {
  override def getConfigurableDescription: String = ???

  override def getLaunchDescription: String = ???

  override def createConfigurable(
    project: Project,
    c: SbtRuntimeTargetConfiguration,
    targetEnvironmentType: TargetEnvironmentType[_],
    supplier: Supplier[TargetEnvironmentConfiguration]
  ): Configurable = ???

  override def findLanguageRuntime(
    targetEnvironmentConfiguration: TargetEnvironmentConfiguration
  ): SbtRuntimeTargetConfiguration = ???

  override def isApplicableTo(runnerAndConfigurationSettings: RunnerAndConfigurationSettings): Boolean = true

  override def getDisplayName: String = "Sbt"

  override def getIcon: Icon = Icons.SBT

  override def createDefaultConfig(): SbtRuntimeTargetConfiguration = SbtRuntimeTargetConfiguration()

  override def createSerializer(c: SbtRuntimeTargetConfiguration): PersistentStateComponent[_] = c

  override def duplicateConfig(c: SbtRuntimeTargetConfiguration): SbtRuntimeTargetConfiguration = null
}

object SbtRuntimeType {
  val TYPE_ID = "SbtRuntime"

  val PROJECT_FOLDER_VOLUME = LanguageRuntimeType.VolumeDescriptor(
    SbtRuntimeType.getClass.getTypeName + ":projectFolder",
    SbtPluginBundle.message("sbt.target.execution.project.folder.label"),
    SbtPluginBundle.message("sbt.target.execution.project.folder.description"),
    SbtPluginBundle.message("sbt.target.execution.project.folder.title"),
    ""
  )
}
