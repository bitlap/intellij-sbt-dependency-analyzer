package bitlap
package sbt
package analyzer
package task

import scala.concurrent.*

import bitlap.sbt.analyzer.*
import bitlap.sbt.analyzer.DependencyUtils.*
import bitlap.sbt.analyzer.model.AnalyzerCommandNotFoundException
import bitlap.sbt.analyzer.model.AnalyzerCommandUnknownException
import bitlap.sbt.analyzer.parser.*

import org.jetbrains.plugins.scala.inWriteAction
import org.jetbrains.sbt.shell.SbtShellCommunication

import com.intellij.buildsystem.model.unified.UnifiedCoordinates
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.dependencies.DependencyScopeNode
import com.intellij.openapi.project.Project

/** Tasks depend on the `addDependencyTreePlugin` plugin of the SBT.
 *  @author
 *    梦境迷离
 *  @version 1.0,2023/8/11
 */
trait SbtShellDependencyAnalysisTask {

  val parserTypeEnum: ParserTypeEnum

  def executeCommand(
    project: Project,
    moduleData: ModuleData,
    scope: DependencyScopeEnum,
    organization: String,
    moduleNamePaths: Map[String, String],
    sbtModules: Map[String, String],
    declared: List[UnifiedCoordinates]
  ): DependencyScopeNode

  protected final def taskCompleteCallback(
    project: Project,
    moduleData: ModuleData,
    scope: DependencyScopeEnum
  )(rootNode: String => DependencyScopeNode): DependencyScopeNode = {
    val comms    = SbtShellCommunication.forProject(project)
    val moduleId = moduleData.getId.split(" ")(0)
    val promise  = Promise[Boolean]()
    val file     = moduleData.getLinkedExternalProjectPath + analysisFilePath(scope, parserTypeEnum)
    val result = comms
      .command(
        scopedKey(moduleId, scope, parserTypeEnum.cmd),
        new StringBuilder(),
        SbtShellCommunication.listenerAggregator {
          case SbtShellCommunication.TaskComplete =>
            if (!promise.isCompleted) {
              promise.success(true)
            }
          case SbtShellCommunication.ErrorWaitForInput =>
            if (!promise.isCompleted) {
              promise.failure(
                AnalyzerCommandUnknownException(
                  parserTypeEnum.cmd,
                  moduleId,
                  scope,
                  SbtDependencyAnalyzerBundle.message("sbt.dependency.analyzer.error.title")
                )
              )
            }
          case SbtShellCommunication.Output(line) =>
            if (line.startsWith(s"[error]") && line.contains(parserTypeEnum.cmd) && !promise.isCompleted) {
              promise.failure(
                AnalyzerCommandNotFoundException(
                  SbtDependencyAnalyzerBundle.message("sbt.dependency.analyzer.error.title")
                )
              )
            } else if (line.startsWith(s"[error]") && !promise.isCompleted) {
              promise.failure(
                AnalyzerCommandUnknownException(
                  parserTypeEnum.cmd,
                  moduleId,
                  scope,
                  SbtDependencyAnalyzerBundle.message("sbt.dependency.analyzer.error.title")
                )
              )
            }
          case SbtShellCommunication.TaskStart =>

        }
      )
      .flatMap(_ => promise.future)

    Await.result(result, Constants.timeout)
    rootNode(file)
  }
}

object SbtShellDependencyAnalysisTask:

  lazy val dependencyDotTask: SbtShellDependencyAnalysisTask = new DependencyDotTask
