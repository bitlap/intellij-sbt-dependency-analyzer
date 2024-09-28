package bitlap
package sbt
package analyzer
package action

import scala.util.Try

import bitlap.sbt.analyzer.*

import org.jetbrains.sbt.project.SbtProjectSystem

import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.dependency.analyzer.*
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement

final class SbtDependencyAnalyzerGoToAction extends DependencyAnalyzerGoToAction(SbtProjectSystem.Id):

  getTemplatePresentation.setText(
    SbtDependencyAnalyzerBundle.message("analyzer.action.gotoAction.text")
  )

  private val LOG = Logger.getInstance(classOf[SbtDependencyAnalyzerGoToAction])

  override def getNavigatable(e: AnActionEvent): Navigatable =
    Option(SbtDependencyAnalyzerActionUtil.getModifiableDependency(e))
      .flatMap(_.declaredDependencies)
      .flatMap { dependency =>
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
  end getNavigatable

end SbtDependencyAnalyzerGoToAction
