package bitlap
package sbt
package analyzer
package util

import java.util.Collections
import java.util.concurrent.atomic.AtomicLong

import scala.jdk.CollectionConverters.*

import bitlap.sbt.analyzer.model.*
import bitlap.sbt.analyzer.parser.*

import org.jetbrains.plugins.scala.extensions.*
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScInfixExpr
import org.jetbrains.plugins.scala.project.*
import org.jetbrains.sbt.language.utils.*
import org.jetbrains.sbt.language.utils.SbtDependencyCommon.defaultLibScope
import org.jetbrains.sbt.language.utils.SbtDependencyUtils.*
import org.jetbrains.sbt.language.utils.SbtDependencyUtils.GetMode.GetDep

import com.intellij.buildsystem.model.DeclaredDependency
import com.intellij.buildsystem.model.unified.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.diagnostic.*
import com.intellij.openapi.externalSystem.dependency.analyzer.DAScope
import com.intellij.openapi.externalSystem.model.project.dependencies.*
import com.intellij.openapi.module.Module
import com.intellij.openapi.module as OpenapiModule
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/7
 */
object DependencyUtils {

  final val DefaultConfiguration = toDAScope("default")

  private final val rootId     = new AtomicLong(0)
  private final val artifactId = new AtomicLong(0)

  private val ArtifactRegex                   = "(.*):(.*):(.*)".r
  private val `ModuleWithScalaRegex`          = "(.*)(_)(.*)".r
  private val `ModuleWithScalaJs0.6Regex`     = "(.*)(_sjs0\\.6_)(.*)".r
  private val `ModuleWithScalaJs1Regex`       = "(.*)(_sjs1_)(.*)".r
  private val `ModuleWithScalaNative0.4Regex` = "(.*)(_native0\\.4_)(.*)".r
  private val `ModuleWithScalaNative0.3Regex` = "(.*)(_native0\\.3)(.*)".r
  private val `ModuleWithScalaNative0.2Regex` = "(.*)(_native0\\.2)(.*)".r

  private final case class PlatformModule(
    module: String,
    platform: String,
    scalaVersion: String
  )

  private val LOG = Logger.getInstance(classOf[DependencyUtils.type])

  def getDeclaredDependency(module: Module): List[DeclaredDependency] = {
    declaredDependencies(module).asScala.toList
  }

  /** self is a ProjectDependencyNodeImpl, because we first convert it to DependencyNode and then filter it. This is
   *  important, for dependency graphs/trees, this is the root node.
   */
  def isSelfModule(dn: DependencyNode, context: ModuleContext): Boolean = {
    dn.getDisplayName match
      case ArtifactRegex(group, artifact, version) =>
        context.organization == group && isSelfArtifact(artifact, context)
      case _ => false
  }

  def getArtifactInfoFromDisplayName(idOpt: Option[Int], displayName: String): Option[ArtifactInfo] = {
    displayName match
      case ArtifactRegex(group, artifact, version) =>
        Some(ArtifactInfo(idOpt.getOrElse(artifactId.getAndIncrement().toInt), group, artifact, version))
      case _ => None
  }

  def toDAScope(name: String): DAScope = DAScope(name, StringUtil.toTitleCase(name))

  /** do not analyze this module
   */
  def canIgnoreModule(module: Module): Boolean = {
    // if module is itself a build module, skip build module
    val isBuildModule = module.isBuildModule
    isBuildModule || module.isSharedSourceModule
  }

  def getScopedCommandKey(project: String, scope: DependencyScopeEnum, cmd: String): String = {
    if (project == null || project.isEmpty) s"$scope / $cmd"
    else s"$project / $scope / $cmd"
  }

  def analysisFilePath(scope: DependencyScopeEnum, parserTypeEnum: ParserTypeEnum): String =
    s"/target/dependencies-${scope.toString.toLowerCase}.${parserTypeEnum.suffix}"

  def createRootScopeNode(dependencyScope: DependencyScopeEnum, project: Project): DependencyScopeNode = {
    val scopeDisplayName = "project " + project.getBasePath + " (" + dependencyScope.toString + ")"
    val node = new DependencyScopeNode(
      rootId.getAndIncrement(),
      dependencyScope.toString,
      scopeDisplayName,
      dependencyScope.toString
    )
    node.setResolutionState(ResolutionState.RESOLVED)
    node
  }

  def appendChildrenAndFixProjectNodes[N <: DependencyNode](
    parentNode: N,
    nodes: Seq[DependencyNode],
    context: ModuleContext
  ): Unit = {
    parentNode.getDependencies.addAll(nodes.asJava)
    val moduleDependencies = nodes.filter(d => isProjectModule(d, context))
    parentNode.getDependencies.removeIf(node => moduleDependencies.exists(_.getId == node.getId))
    val mds = moduleDependencies.map(d => toProjectDependencyNode(d, context)).collect { case Some(value) =>
      value
    }
    parentNode.getDependencies.addAll(mds.asJava)

    mds.filter(_.isInstanceOf[ArtifactDependencyNodeImpl]).foreach { node =>
      val artifact   = getArtifactInfoFromDisplayName(None, node.getDisplayName)
      val artifactId = artifact.map(_.artifact).getOrElse(Constants.EmptyString)
      val group      = artifact.map(_.group).getOrElse(Constants.EmptyString)
      // Use artifact to determine whether there are modules in the dependency.
      if (
        context.ideaModuleIdSbtModuleNames.values
          .exists(d => group == context.organization && toPlatformModule(artifactId).module == d)
      ) {
        appendChildrenAndFixProjectNodes(
          node,
          node.getDependencies.asScala.toList,
          context
        )
      }
    }
  }

  private def isSelfArtifact(artifact: String, context: ModuleContext): Boolean = {
    // Processing cross platform, module name is not artifact!
    val currentModuleName =
      context.ideaModuleIdSbtModuleNames.getOrElse(
        context.currentModuleId,
        context.ideaModuleIdSbtModuleNames.getOrElse(
          Constants.SingleSbtModule,
          context.ideaModuleIdSbtModuleNames.getOrElse(Constants.RootSbtModule, context.currentModuleId)
        )
      )

    // NOTE: we don't determine the Scala version number.
    if (context.isScalaNative) {
      artifact match
        case `ModuleWithScalaNative0.4Regex`(module, _, _) =>
          currentModuleName == module
        case `ModuleWithScalaNative0.3Regex`(module, _, _) =>
          currentModuleName == module
        case `ModuleWithScalaNative0.2Regex`(module, _, _) =>
          currentModuleName == module
        case _ => false

    } else if (context.isScalaJs) {
      artifact match
        case `ModuleWithScalaJs0.6Regex`(module, _, _) =>
          currentModuleName == module
        case `ModuleWithScalaJs1Regex`(module, _, _) =>
          currentModuleName == module
        case _ => false

    } else {
      artifact match
        case `ModuleWithScalaRegex`(module, _, _) =>
          currentModuleName == module
        // it is a java project
        case _ => artifact == currentModuleName
    }
  }

  private def toPlatformModule(artifact: String): PlatformModule = {
    artifact match
      case `ModuleWithScalaJs0.6Regex`(module, _, scalaVer)     => PlatformModule(module, "sjs0.6", scalaVer)
      case `ModuleWithScalaJs1Regex`(module, _, scalaVer)       => PlatformModule(module, "sjs1", scalaVer)
      case `ModuleWithScalaNative0.4Regex`(module, _, scalaVer) => PlatformModule(module, "native0.4", scalaVer)
      case `ModuleWithScalaNative0.3Regex`(module, _, scalaVer) => PlatformModule(module, "native0.3", scalaVer)
      case `ModuleWithScalaNative0.2Regex`(module, _, scalaVer) => PlatformModule(module, "native0.2", scalaVer)
      case `ModuleWithScalaRegex`(module, _, scalaVer)          => PlatformModule(module, "", scalaVer)
      case _                                                    => PlatformModule(artifact, "", "")
  }

  private def toProjectDependencyNode(dn: DependencyNode, context: ModuleContext): Option[DependencyNode] = {
    val artifactInfo = getArtifactInfoFromDisplayName(Some(dn.getId.toInt), dn.getDisplayName).orNull
    if (artifactInfo == null) return None
    val sbtModuleName  = toPlatformModule(artifactInfo.artifact).module
    val ideaModuleName = context.ideaModuleIdSbtModuleNames.find(_._2 == sbtModuleName).map(_._1)

    // Processing cross platform, module name is not artifact
    // This is a project node, we need a module not a artifact to get project path!
    val projectPath =
      ideaModuleName
        .flatMap(m => context.ideaModuleNamePaths.get(m))
        .getOrElse(context.ideaModuleNamePaths.getOrElse(sbtModuleName, Constants.EmptyString))

    val p = new ProjectDependencyNodeImpl(
      dn.getId,
      sbtModuleName,
      projectPath
    )
    if (p.getProjectPath.isEmpty) {
      p.setResolutionState(ResolutionState.UNRESOLVED)
    } else {
      p.setResolutionState(ResolutionState.RESOLVED)
    }
    p.getDependencies.addAll(
      dn.getDependencies.asScala
        .filterNot(d => isSelfModule(d, context.copy(currentModuleId = sbtModuleName)))
        .asJava
    )
    Some(p)
  }

  private def isProjectModule(dn: DependencyNode, context: ModuleContext): Boolean = {
    // module dependency
    val artifactInfo = getArtifactInfoFromDisplayName(Some(dn.getId.toInt), dn.getDisplayName).orNull
    if (artifactInfo == null) return false
    if (artifactInfo.group != context.organization) return false
    // Use artifact to determine whether there are modules in the dependency.
    val matchModule =
      context.ideaModuleIdSbtModuleNames.values.filter(m => m == toPlatformModule(artifactInfo.artifact).module)

    matchModule.nonEmpty

  }

  /** copy from DependencyModifierService, and fix
   */
  def declaredDependencies(module: OpenapiModule.Module): java.util.List[DeclaredDependency] = try {
    // Check whether the IDE is in Dumb Mode. If it is, return empty list instead proceeding
    // if (DumbService.getInstance(module.getProject).isDumb) return Collections.emptyList()
    val scalaVer = SbtDependencyUtils.getScalaVerFromModule(module)

    inReadAction({
      val libDeps = SbtDependencyUtils
        .getLibraryDependenciesOrPlaces(getSbtFileOpt(module), module.getProject, module, GetDep)
        .map(_.asInstanceOf[(ScInfixExpr, String, ScInfixExpr)])
      libDeps
        .map(libDepInfixAndString => {
          val libDepArr = SbtDependencyUtils
            .processLibraryDependencyFromExprAndString(libDepInfixAndString)
            .map(_.asInstanceOf[String])
          val dataContext: DataContext = (dataId: String) => {
            if (CommonDataKeys.PSI_ELEMENT.is(dataId)) {
              libDepInfixAndString
            } else null
          }

          libDepArr.length match {
            case x if x == 2 =>
              val scope = SbtDependencyCommon.defaultLibScope
              // if version is a val, not a string, cannot get it
              if (isScalaLibraryDependency(libDepInfixAndString._1))
                new DeclaredDependency(
                  new UnifiedDependency(
                    libDepArr(0),
                    SbtDependencyUtils.buildScalaArtifactIdString(libDepArr(0), libDepArr(1), scalaVer),
                    scope,
                    scope
                  ),
                  dataContext
                )
              else
                new DeclaredDependency(
                  new UnifiedDependency(libDepArr(0), libDepArr(1), scope, scope),
                  dataContext
                )
            case x if x < 3 || x > 4 => null
            case x if x >= 3 =>
              val scope = if (x == 3) SbtDependencyCommon.defaultLibScope else libDepArr(3)
              if (isScalaLibraryDependency(libDepInfixAndString._1))
                new DeclaredDependency(
                  new UnifiedDependency(
                    libDepArr(0),
                    SbtDependencyUtils.buildScalaArtifactIdString(libDepArr(0), libDepArr(1), scalaVer),
                    libDepArr(2),
                    scope
                  ),
                  dataContext
                )
              else
                new DeclaredDependency(
                  new UnifiedDependency(libDepArr(0), libDepArr(1), libDepArr(2), scope),
                  dataContext
                )
          }
        })
        .filter(_ != null)
        .toList
        .asJava
    })
  } catch {
    case c: ControlFlowException => throw c
    case e: Exception =>
      LOG.error(
        s"Error occurs when obtaining the list of dependencies for module ${module.getName} using package search plugin",
        e
      )
      Collections.emptyList()
  }

}
