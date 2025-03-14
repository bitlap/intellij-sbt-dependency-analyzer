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
      if (modifiableDependency.parentDependency.getParent != null) {
        val unifiedDependency =
          new UnifiedDependency(parent, modifiableDependency.parentDependency.getParent.getScope.getTitle)
        val coordinates: UnifiedCoordinates = modifiableDependency.coordinates
        if (coordinates == parent) {
          try {
            // remove declared dependency
            SbtDependencyModifier.removeDependency(modifiableDependency.module, unifiedDependency)
            Notifications.notifyDependencyChanged(
              modifiableDependency.module.getProject,
              coordinates.getDisplayName,
              self = true
            )
          } catch {
            case e: Exception =>
              LOG.error(s"Cannot remove declared dependency: ${coordinates.getDisplayName}", e)
              Notifications.notifyDependencyChanged(
                modifiableDependency.module.getProject,
                coordinates.getDisplayName,
                self = true,
                success = false
              )
          }

        } else {
          // add exclude coordinates
          val ret =
            SbtDependencyModifier.addExcludeToDependency(modifiableDependency.module, unifiedDependency, coordinates)
          if (ret) {
            Notifications.notifyDependencyChanged(
              modifiableDependency.module.getProject,
              coordinates.getDisplayName,
              success = true,
              self = false
            )
          } else {
            Notifications.notifyDependencyChanged(
              modifiableDependency.module.getProject,
              coordinates.getDisplayName,
              success = false,
              self = false
            )
          }
        }
      }
    }
  }

end SbtDependencyAnalyzerExcludeAction
