package bitlap.intellij.analyzer.configuration

import org.jetbrains.sbt.project.*
import org.jetbrains.sbt.runner.SbtRunConfiguration as ISbtRunConfiguration

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.service.execution.*
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/2
 */
final class SbtExternalTaskConfigurationType extends AbstractExternalSystemTaskConfigurationType(SbtProjectSystem.Id) {

  override def getHelpTopic = "reference.dialogs.rundebug.SbtRunConfiguration"

  override def getConfigurationFactoryId: String = "Sbt"

  override def doCreateConfiguration(
    externalSystemId: ProjectSystemId,
    project: Project,
    factory: ConfigurationFactory,
    name: String
  ): ExternalSystemRunConfiguration =
    new SbtRunConfiguration(new ISbtRunConfiguration(project, factory, name))

  override def isDumbAware = true

  override protected def isEditableInDumbMode = true

}

object SbtExternalTaskConfigurationType {

  def getInstance: SbtExternalTaskConfigurationType =
    ExternalSystemUtil.findConfigurationType(SbtProjectSystem.Id).asInstanceOf[SbtExternalTaskConfigurationType]

}
