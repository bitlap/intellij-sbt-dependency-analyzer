package bitlap.sbt.analyzer.action

import bitlap.sbt.analyzer.*
import bitlap.sbt.analyzer.util.packagesearch.*

import com.intellij.buildsystem.model.unified.{ UnifiedCoordinates, UnifiedDependency }
import com.intellij.openapi.actionSystem.*

final class SbtDependencyAnalyzerExcludeAction extends BaseRefreshDependenciesAction:

  override lazy val eventText: String = SbtDependencyAnalyzerBundle.message("analyzer.action.excludeAction.text")

  override lazy val eventDescription: String = eventText

  override def actionPerformed(e: AnActionEvent): Unit = {
    Option(SbtDependencyAnalyzerActionUtil.getModifiableDependency(e)).foreach { modifiableDependency =>
      val parent = getUnifiedCoordinates(modifiableDependency.parentDependency)
      val unifiedDependency =
        new UnifiedDependency(parent, modifiableDependency.parentDependency.getParent.getScope.getTitle)
      val coordinates: UnifiedCoordinates = modifiableDependency.coordinates
      if (coordinates == parent) {
        // remove declared dependency
        SbtDependencyModifier.removeDependency(modifiableDependency.module, unifiedDependency)
      } else {
        // add exclude coordinates
        SbtDependencyModifier.addExcludeToDependency(modifiableDependency.module, unifiedDependency, coordinates)
      }
    }
  }

end SbtDependencyAnalyzerExcludeAction
