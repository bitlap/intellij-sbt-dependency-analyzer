package bitlap.intellij.analyzer

import org.jetbrains.sbt.project.SbtProjectSystem

import com.intellij.buildsystem.model.DeclaredDependency
import com.intellij.externalSystem.DependencyModifierService
import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerGoToAction
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerView
import com.intellij.pom.Navigatable
import scala.jdk.CollectionConverters.*

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/1
 */
final class SbtDependencyAnalyzerGoToAction extends DependencyAnalyzerGoToAction(SbtProjectSystem.Id) {

  override def getNavigatable(e: AnActionEvent): Navigatable = {
    val dependency = getDeclaredDependency(e)
    if (dependency == null) return null
    val psiElement = dependency.psiElement
    if (psiElement == null) return null
    val navigationSupport = PsiNavigationSupport.getInstance()
    navigationSupport.getDescriptor(psiElement)
  }

  private def getDeclaredDependency(e: AnActionEvent): DeclaredDependency = {
    val project = e.project
    if (project == null) return null
    val dependency = e.getData(DependencyAnalyzerView.DEPENDENCY)
    if (dependency == null) return null
    val coordinates = getUnifiedCoordinates(dependency)
    if (coordinates == null) return null
    val module = getParentModule(project, dependency)
    if (module == null) return null
    val dependencyModifierService = DependencyModifierService.getInstance(project)
    dependencyModifierService.declaredDependencies(module).asScala.find(_.coordinates == coordinates).orNull
  }

}
