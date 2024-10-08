package bitlap
package sbt
package analyzer
package task

import bitlap.sbt.analyzer.model.*
import bitlap.sbt.analyzer.parser.*
import bitlap.sbt.analyzer.util.DependencyUtils.*

import org.jetbrains.plugins.scala.project.ModuleExt

import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.dependencies.DependencyScopeNode
import com.intellij.openapi.project.Project

/** Process the `sbt dependencyDot` command, when the command execution is completed, use a callback to parse the file
 *  content.
 */
final class DependencyDotTask extends SbtShellDependencyAnalysisTask:

  override val parserTypeEnum: AnalyzedFileType = AnalyzedFileType.Dot

  override def executeCommand(
    project: Project,
    moduleData: ModuleData,
    scope: DependencyScopeEnum,
    organization: String,
    moduleNamePaths: Map[String, String],
    ideaModuleIdSbtModules: Map[String, String]
  ): DependencyScopeNode =
    val module   = findModule(project, moduleData)
    val moduleId = moduleData.getId.split(" ")(0)

    taskCompleteCallback(project, moduleData, scope) { file =>
      val sbtModuleNameMap =
        if (ideaModuleIdSbtModules.isEmpty) Map(moduleId -> module.getName)
        else ideaModuleIdSbtModules
      AnalyzedParserFactory
        .getInstance(parserTypeEnum)
        .buildDependencyTree(
          ModuleContext(
            file,
            moduleId,
            scope,
            organization,
            moduleNamePaths,
            module.isScalaJs,
            module.isScalaNative,
            sbtModuleNameMap
          ),
          createRootScopeNode(scope, project)
        )
    }
  end executeCommand

end DependencyDotTask
