package bitlap.intellij.analyzer

import scala.jdk.CollectionConverters.*

import org.jetbrains.sbt.project.SbtProjectSystem

import com.intellij.buildsystem.model.DeclaredDependency
import com.intellij.externalSystem.DependencyModifierService
import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerGoToAction
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerView
import com.intellij.pom.Navigatable

final class SbtDependencyAnalyzerGoToAction extends DependencyAnalyzerGoToAction(SbtProjectSystem.Id) {

  override def getNavigatable(e: AnActionEvent): Navigatable = {
    Option(getDeclaredDependency(e))
      .flatMap(dependency => Option(dependency.getPsiElement))
      .map(psiElement => PsiNavigationSupport.getInstance().getDescriptor(psiElement))
      .orNull
  }

  private def getDeclaredDependency(e: AnActionEvent): DeclaredDependency = {
    val project = e.getProject
    val dependency = e.getData(DependencyAnalyzerView.Companion.getDEPENDENCY)
    if (project == null || dependency == null) return null

    val coordinates = getUnifiedCoordinates(dependency)
    val module = getParentModule(project, dependency)
    if (coordinates == null || module == null) return null

    val dependencyModifierService = DependencyModifierService.getInstance(project)
    dependencyModifierService.declaredDependencies(module).asScala.find(_.getCoordinates == coordinates).orNull
  }
}
