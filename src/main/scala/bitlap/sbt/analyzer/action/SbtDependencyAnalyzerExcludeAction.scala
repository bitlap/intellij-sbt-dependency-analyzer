package bitlap.sbt.analyzer.action

import bitlap.sbt.analyzer.*
import bitlap.sbt.analyzer.model.AnalyzerCommandNotFoundException
import bitlap.sbt.analyzer.util.*
import bitlap.sbt.analyzer.util.packagesearch.*

import com.intellij.buildsystem.model.unified.{ UnifiedCoordinates, UnifiedDependency }
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.diagnostic.Logger

final class SbtDependencyAnalyzerExcludeAction extends BaseRefreshDependenciesAction:

  override lazy val eventText: String = SbtDependencyAnalyzerBundle.message("analyzer.action.excludeAction.text")

  override lazy val eventDescription: String =
    SbtDependencyAnalyzerBundle.message("analyzer.action.excludeAction.description")
  private val LOG = Logger.getInstance(classOf[SbtDependencyAnalyzerExcludeAction])

  override def actionPerformed(e: AnActionEvent): Unit = {
    Option(SbtDependencyAnalyzerActionUtil.getModifiableDependency(e)).foreach { modifiableDependency =>
      val parent = getUnifiedCoordinates(modifiableDependency.parentDependency)
      val unifiedDependency =
        new UnifiedDependency(parent, modifiableDependency.parentDependency.getParent.getScope.getTitle)
      val coordinates: UnifiedCoordinates = modifiableDependency.coordinates
      if (coordinates == parent) {
        // remove declared dependency
        try {
          SbtDependencyModifier.removeDependency(modifiableDependency.module, unifiedDependency)
          Notifications.notifyDependencyChanged(
            modifiableDependency.module.getProject,
            coordinates.getDisplayName,
            true
          )
        } catch {
          case e: AnalyzerCommandNotFoundException =>
            LOG.error(s"Cannot remove declared dependency: ${coordinates.getDisplayName}", e)
          case ignore: Exception => throw ignore
        }

      } else {
        // add exclude coordinates
        val ret =
          SbtDependencyModifier.addExcludeToDependency(modifiableDependency.module, unifiedDependency, coordinates)
        if (ret) {
          Notifications.notifyDependencyChanged(
            modifiableDependency.module.getProject,
            coordinates.getDisplayName,
            false
          )
        }
      }
    }
  }

end SbtDependencyAnalyzerExcludeAction
