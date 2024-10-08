package bitlap.sbt.analyzer.activity

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

import kotlin.coroutines.Continuation

abstract class BaseProjectActivity(private val runOnlyOnce: Boolean = false) extends ProjectActivity {
  private var veryFirstProjectOpening: Boolean = true

  override def execute(project: Project, continuation: Continuation[? >: kotlin.Unit]): AnyRef = {
    if (
      ApplicationManager.getApplication.isUnitTestMode || (runOnlyOnce && !veryFirstProjectOpening) || project.isDisposed
    ) {
      return continuation
    }
    // FIXME: should use continuation
    veryFirstProjectOpening = false
    if (onBeforeRunActivity(project)) {
      onRunActivity(project)
    }
    continuation
  }

  private def onBeforeRunActivity(project: Project): Boolean = true

  protected def onRunActivity(project: Project): Unit

}
