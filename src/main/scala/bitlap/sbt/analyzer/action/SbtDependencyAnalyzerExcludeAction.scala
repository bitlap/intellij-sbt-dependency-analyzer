package bitlap.sbt.analyzer.action

import scala.jdk.CollectionConverters.*
import scala.util.Try

import bitlap.sbt.analyzer.*
import bitlap.sbt.analyzer.util.packagesearch.*

import org.jetbrains.sbt.project.SbtProjectSystem

import com.intellij.buildsystem.model.DeclaredDependency
import com.intellij.buildsystem.model.unified.{ UnifiedCoordinates, UnifiedDependency }
import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.dependency.analyzer.*
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement

final class SbtDependencyAnalyzerExcludeAction extends BaseRefreshDependenciesAction:

  override lazy val eventText: String = SbtDependencyAnalyzerBundle.message("analyzer.action.excludeAction.text")

  override lazy val eventDescription: String = eventText

  override def actionPerformed(e: AnActionEvent): Unit = {
    Option(SbtDependencyAnalyzerActionUtil.getModifiableDependency(e)).foreach { modifiableDependency =>
      val parent = getUnifiedCoordinates(
        modifiableDependency.parentDependency
      )
      val unifiedDependency =
        new UnifiedDependency(parent, modifiableDependency.parentDependency.getParent.getScope.getTitle)
        // remove declared dependency, and add again with `exclude("javax.jms", "jms")`
      SbtDependencyModifier.removeDependency(modifiableDependency.module, unifiedDependency)
    }
  }

end SbtDependencyAnalyzerExcludeAction
