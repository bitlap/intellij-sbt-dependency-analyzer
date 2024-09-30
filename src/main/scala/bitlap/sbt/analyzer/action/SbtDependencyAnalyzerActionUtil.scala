package bitlap.sbt.analyzer.action

import scala.jdk.CollectionConverters.*

import bitlap.sbt.analyzer.*
import bitlap.sbt.analyzer.util.DependencyUtils

import com.intellij.buildsystem.model.DeclaredDependency
import com.intellij.buildsystem.model.unified.UnifiedCoordinates
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.externalSystem.dependency.analyzer.*
import com.intellij.openapi.module.Module as OpenapiModule

final case class ModifiableDependency(
  module: OpenapiModule,
  coordinates: UnifiedCoordinates,
  declaredDependency: Option[DeclaredDependency],
  candidateDeclaredDependencies: List[DeclaredDependency],
  parentDependency: DependencyAnalyzerDependency
)

object SbtDependencyAnalyzerActionUtil {

  def getModifiableDependency(e: AnActionEvent): ModifiableDependency =
    val project    = e.getProject
    val dependency = e.getData(DependencyAnalyzerView.Companion.getDEPENDENCY)
    if (project == null || dependency == null) return null

    val coordinates: UnifiedCoordinates = getUnifiedCoordinates(dependency)
    val parentDependencyAndModule       = getParentModule(project, dependency)
    if (coordinates == null || parentDependencyAndModule == null) return null

    val (parentDependency, module) = parentDependencyAndModule

    val candidateDeclaredDependencies = DependencyUtils
      .getDeclaredDependency(module)
    val declared = candidateDeclaredDependencies.find(dc =>
      // hard code, see SbtDependencyUtils#getLibraryDependenciesOrPlacesFromPsi
      val artifactName =
        if (
          coordinates.getArtifactId.endsWith("_3") || coordinates.getArtifactId.endsWith("_2.13") ||
          coordinates.getArtifactId.endsWith("_2.12") || coordinates.getArtifactId.endsWith("_2.11")
        ) coordinates.getArtifactId.split('_').head
        else coordinates.getArtifactId
      (dc.getCoordinates.getArtifactId == coordinates.getArtifactId ||
        dc.getCoordinates.getArtifactId == artifactName ||
        // maybe a fixed artifact
        dc.getCoordinates.getVersion == artifactName) &&
        dc.getCoordinates.getGroupId == coordinates.getGroupId
    )

    ModifiableDependency(module, coordinates, declared, candidateDeclaredDependencies, parentDependency)
  end getModifiableDependency

}
