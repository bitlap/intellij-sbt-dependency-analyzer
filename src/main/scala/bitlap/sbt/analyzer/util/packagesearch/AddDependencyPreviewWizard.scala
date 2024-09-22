package bitlap
package sbt
package analyzer
package util
package packagesearch

import org.jetbrains.sbt.language.utils.{ DependencyOrRepositoryPlaceInfo, SbtArtifactInfo }

import com.intellij.ide.wizard.{ AbstractWizard, Step }
import com.intellij.openapi.project.Project

class AddDependencyPreviewWizard(
  project: Project,
  elem: SbtArtifactInfo,
  fileLines: Seq[DependencyOrRepositoryPlaceInfo]
) extends AbstractWizard[Step](
      SbtDependencyAnalyzerBundle.message(
        "analyzer.packagesearch.dependency.sbt.possible.places.to.add.new.dependency"
      ),
      project
    ) {

  private val sbtPossiblePlacesStep = new SbtPossiblePlacesStep(this, project, fileLines)

  val elementToAdd: Any                                       = elem
  var resultFileLine: Option[DependencyOrRepositoryPlaceInfo] = scala.compiletime.uninitialized

  override def getHelpID: String = null

  def search(): Option[DependencyOrRepositoryPlaceInfo] = {
    if (!showAndGet()) {
      return None
    }
    resultFileLine
  }

  addStep(sbtPossiblePlacesStep)
  init()

  override def dispose(): Unit = {
    sbtPossiblePlacesStep.panel.releaseEditor()
    super.dispose()
  }
}
