package bitlap.sbt.analyzer

import scala.jdk.CollectionConverters.*
import scala.util.Try

import org.jetbrains.sbt.project.SbtProjectSystem

import com.intellij.buildsystem.model.DeclaredDependency
import com.intellij.buildsystem.model.unified.UnifiedCoordinates
import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.dependency.analyzer.*
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement

final class SbtDependencyAnalyzerGoToAction extends DependencyAnalyzerGoToAction(SbtProjectSystem.Id) {

  private val LOG = Logger.getInstance(classOf[SbtDependencyAnalyzerGoToAction])

  override def getNavigatable(e: AnActionEvent): Navigatable = {
    Option(getDeclaredDependency(e)).flatMap { dependency =>
      Try {
        val data = dependency.getDataContext.getData(CommonDataKeys.PSI_ELEMENT.getName)
        data match
          case t: (_, _, _) if t._1.isInstanceOf[PsiElement] =>
            Some(t._1.asInstanceOf[PsiElement])
          case _ => None
      }.getOrElse {
        LOG.error(s"Cannot get 'PSI_ELEMENT' as 'PsiElement' for ${dependency.getCoordinates}")
        None
      }
    }
      .map(psiElement => PsiNavigationSupport.getInstance().getDescriptor(psiElement))
      .orNull
  }

  private def getDeclaredDependency(e: AnActionEvent): DeclaredDependency = {
    val project    = e.getProject
    val dependency = e.getData(DependencyAnalyzerView.Companion.getDEPENDENCY)
    if (project == null || dependency == null) return null

    val coordinates: UnifiedCoordinates = getUnifiedCoordinates(dependency)
    val module                          = getParentModule(project, dependency)
    if (coordinates == null || module == null) return null

    val declared = DependencyUtils.getDeclaredDependency(module)
    declared
      .find(dc =>
        if (
          DependencyScopeEnum.values.exists(d =>
            d.toString
              .toLowerCase() == dc.getCoordinates.getVersion.toLowerCase || Constants.Protobuf == dc.getCoordinates.getVersion.toLowerCase
          )
        ) {
          dc.getCoordinates.getArtifactId == coordinates.getArtifactId && dc.getCoordinates.getGroupId == coordinates.getGroupId
        } else {
          dc.getCoordinates.equals(coordinates)
        }
      )
      .orNull
  }
}
