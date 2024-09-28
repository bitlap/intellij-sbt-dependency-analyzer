package bitlap.sbt.analyzer.action

import bitlap.sbt.analyzer.*
import bitlap.sbt.analyzer.util.packagesearch.*

import com.intellij.buildsystem.model.unified.UnifiedDependency
import com.intellij.openapi.actionSystem.*

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
