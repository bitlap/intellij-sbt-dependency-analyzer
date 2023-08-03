package bitlap.sbt.analyzer

import org.jetbrains.sbt.project.SbtProjectSystem

import com.intellij.openapi.Disposable
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerContributor
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerExtension
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.project.Project

final class SbtDependencyAnalyzerExtension extends DependencyAnalyzerExtension {

  override def isApplicable(systemId: ProjectSystemId): Boolean =
    systemId == SbtProjectSystem.Id

  override def createContributor(project: Project, parentDisposable: Disposable): DependencyAnalyzerContributor =
    SbtDependencyAnalyzerContributor(project)
}
