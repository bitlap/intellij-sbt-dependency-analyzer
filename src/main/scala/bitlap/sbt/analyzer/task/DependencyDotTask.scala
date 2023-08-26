package bitlap.sbt.analyzer.task

import scala.concurrent.*

import bitlap.sbt.analyzer.*
import bitlap.sbt.analyzer.Constants.*
import bitlap.sbt.analyzer.DependencyUtils.*
import bitlap.sbt.analyzer.model.*
import bitlap.sbt.analyzer.parser.*
import bitlap.sbt.analyzer.parser.ParserTypeEnum

import org.jetbrains.plugins.scala.project.ModuleExt
import org.jetbrains.sbt.shell.SbtShellCommunication

import com.intellij.buildsystem.model.unified.UnifiedCoordinates
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.dependencies.DependencyScopeNode
import com.intellij.openapi.project.Project

/** Process the `sbt dependencyDot` command, when the command execution is completed, use a callback to parse the file
 *  content.
 *  @author
 *    梦境迷离
 *  @version 1.0,2023/8/19
 */
final class DependencyDotTask extends SbtShellDependencyAnalysisTask {

  override val parserTypeEnum: ParserTypeEnum = ParserTypeEnum.DOT

  override def executeCommand(
    project: Project,
    moduleData: ModuleData,
    scope: DependencyScopeEnum,
    organization: String,
    moduleNamePaths: Map[String, String],
    ideaModuleIdSbtModules: Map[String, String],
    declared: List[UnifiedCoordinates]
  ): DependencyScopeNode = {
    val module   = findModule(project, moduleData)
    val moduleId = moduleData.getId.split(" ")(0)

    taskCompleteCallback(project, moduleData, scope) { file =>
      val sbtModuleNameMap =
        if (ideaModuleIdSbtModules.isEmpty) Map(moduleId -> module.getName)
        else ideaModuleIdSbtModules
      DependencyParserFactory
        .getInstance(parserTypeEnum)
        .buildDependencyTree(
          ModuleContext(
            file,
            moduleId,
            scope,
            scalaMajorVersion(module),
            organization,
            moduleNamePaths,
            module.isScalaJs,
            module.isScalaNative,
            sbtModuleNameMap
          ),
          rootNode(scope, project),
          declared
        )
    }
  }

}
