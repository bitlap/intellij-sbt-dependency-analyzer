package bitlap.intellij.analyzer.configuration

import org.jdom.Element
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.plugins.gradle.GradleIdeManager
import org.jetbrains.plugins.gradle.execution.target.GradleRuntimeType
import org.jetbrains.plugins.gradle.util.GradleBundle
import org.jetbrains.plugins.gradle.util.GradleCommandLine
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.sbt.SbtUtil
import org.jetbrains.sbt.project.*
import org.jetbrains.sbt.runner.SbtRunConfiguration as ISbtRunConfiguration

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableRunConfigurationOptions
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.target.LanguageRuntimeType
import com.intellij.execution.target.TargetEnvironmentAwareRunProfile
import com.intellij.execution.target.TargetEnvironmentConfiguration
import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.sm.runner.SMRunnerConsolePropertiesProvider
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.options.SettingsEditorGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.WriteExternalException
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.text.StringUtil

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/2
 */
final class SbtRunConfiguration(sbtRunConfiguration: ISbtRunConfiguration)
    extends ExternalSystemRunConfiguration(
      SbtProjectSystem.Id,
      sbtRunConfiguration.project,
      sbtRunConfiguration.configurationFactory,
      sbtRunConfiguration.name
    ),
      SMRunnerConsolePropertiesProvider,
      TargetEnvironmentAwareRunProfile {

  override def canRunOn(target: TargetEnvironmentConfiguration): Boolean = true

  override def getDefaultLanguageRuntimeType: LanguageRuntimeType[_] = {
    LanguageRuntimeType.EXTENSION_NAME.findExtension(classOf[SbtRuntimeType])
  }

  override def getDefaultTargetName: String = getOptions.getRemoteTarget

  override def setDefaultTargetName(targetName: String): Unit = getOptions.setRemoteTarget(targetName)

  override def getState(executor: Executor, env: ExecutionEnvironment): RunProfileState =
    sbtRunConfiguration.getState(executor, env)

  override def getOptions: LocatableRunConfigurationOptions = super.getOptions

  override def readExternal(element: Element): Unit = sbtRunConfiguration.readExternal(element)

  override def writeExternal(element: Element): Unit = sbtRunConfiguration.writeExternal(element)

  override def getConfigurationEditor: SettingsEditor[ExternalSystemRunConfiguration] =
    super.getConfigurationEditor

  override def createTestConsoleProperties(executor: Executor): SMTRunnerConsoleProperties = null

  override def clone(): ExternalSystemRunConfiguration = this
}
