package bitlap.sbt.analyzer

import java.util.Collections

import scala.jdk.CollectionConverters.*

import bitlap.sbt.analyzer.model.*
import bitlap.sbt.analyzer.parser.DOTDependencyParser.id

import org.jetbrains.plugins.scala.extensions.*
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScInfixExpr
import org.jetbrains.sbt.language.utils.{ SbtDependencyCommon, SbtDependencyUtils }
import org.jetbrains.sbt.language.utils.SbtDependencyCommon.defaultLibScope
import org.jetbrains.sbt.language.utils.SbtDependencyUtils.*
import org.jetbrains.sbt.language.utils.SbtDependencyUtils.GetMode.GetDep

import com.intellij.buildsystem.model.DeclaredDependency
import com.intellij.buildsystem.model.unified.{ UnifiedCoordinates, UnifiedDependency }
import com.intellij.openapi.actionSystem.{ CommonDataKeys, DataContext }
import com.intellij.openapi.diagnostic.{ ControlFlowException, Logger }
import com.intellij.openapi.externalSystem.model.project.dependencies.*
import com.intellij.openapi.module.Module
import com.intellij.openapi.module as OpenapiModule
import com.intellij.openapi.project.Project

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/7
 */
object DependencyUtil {

  private val ArtifactRegex                 = "(.*):(.*):(.*)".r
  private val ScalaVerRegex                 = "(.*)\\.(.*)\\.(.*)".r
  private val `ModuleWithScalaRegex`        = "(.*)(_)(.*)".r
  private val `ModuleWithScalaJs0.6Regex`   = "(.*)(_sjs0\\.6_)(.*)".r
  private val `ModuleWithScalaJs1Regex`     = "(.*)(_sjs1_)(.*)".r
  private val `ModuleWithScalaNative1Regex` = "(.*)(_native0\\.4_)(.*)".r

  private final case class PlatformModule(
    module: String,
    platform: String,
    scalaVersion: String
  )

  private val LOG = Logger.getInstance(classOf[DependencyUtil.type])

  def getDeclaredDependency(module: Module, project: Project): List[DeclaredDependency] = {
    declaredDependencies(module).asScala.toList
  }

  def getUnifiedCoordinates(module: Module, project: Project): List[UnifiedCoordinates] = {
    getDeclaredDependency(module, project).map(_.getCoordinates)
  }

  def scalaMajorVersion(module: Module): String = {
    val scalaVer = SbtDependencyUtils.getScalaVerFromModule(module)
    scalaVer match
      case ScalaVerRegex(major, minor, fix) if major == "2" => s"$major.$minor"
      case _                                                => "3"
  }

  /** get self module
   *
   *  self is a ProjectDependencyNodeImpl, because we first convert it to DependencyNode and then filter it.
   */
  def isSelfProjectModule(dn: DependencyNode, context: ModuleContext): Boolean = {
    dn.getDisplayName match
      case ArtifactRegex(group, artifact, version) =>
        context.org == group && isCurrentModule(artifact, context)
      case _ => false
  }

  def extractArtifactFromName(idOpt: Option[Int], name: String): Option[ArtifactInfo] = {
    name match
      case ArtifactRegex(group, artifact, version) =>
        Some(ArtifactInfo(idOpt.getOrElse(id.getAndIncrement()), group, artifact, version))
      case _ => None
  }

  def isCurrentModule(artifact: String, context: ModuleContext): Boolean = {
    if (context.isScalaNative) {
      artifact match
        case `ModuleWithScalaNative1Regex`(module, _, scalaVer) =>
          context.currentModuleName == module && scalaVer == context.scalaMajor
        case _ => false

    } else if (context.isScalaJs) {
      artifact match
        case `ModuleWithScalaJs0.6Regex`(module, _, scalaVer) =>
          context.currentModuleName == module && scalaVer == context.scalaMajor
        case `ModuleWithScalaJs1Regex`(module, _, scalaVer) =>
          context.currentModuleName == module && scalaVer == context.scalaMajor
        case _ => false

    } else {
      artifact match
        case `ModuleWithScalaRegex`(module, _, scalaVer) =>
          context.currentModuleName == module && scalaVer == context.scalaMajor
        case _ => false
    }
  }

  private def toPlatformModule(artifact: String): PlatformModule = {
    artifact match
      case `ModuleWithScalaRegex`(module, _, scalaVer)        => PlatformModule(module, "", scalaVer)
      case `ModuleWithScalaJs0.6Regex`(module, _, scalaVer)   => PlatformModule(module, "sjs0.6", scalaVer)
      case `ModuleWithScalaJs1Regex`(module, _, scalaVer)     => PlatformModule(module, "sjs1", scalaVer)
      case `ModuleWithScalaNative1Regex`(module, _, scalaVer) => PlatformModule(module, "native0.4", scalaVer)
      case _                                                  => PlatformModule(artifact, "", "")
  }

  private def toProjectDependencyNode(dn: DependencyNode, context: ModuleContext): Option[DependencyNode] = {
    val artifactInfo = extractArtifactFromName(Some(dn.getId.toInt), dn.getDisplayName).orNull
    if (artifactInfo == null) return None
    val moduleName = toPlatformModule(artifactInfo.artifact).module

    val p = new ProjectDependencyNodeImpl(
      dn.getId,
      moduleName,
      context.allModulePaths.getOrElse(moduleName, Constants.Empty_String)
    )
    if (p.getProjectPath.isEmpty) {
      p.setResolutionState(ResolutionState.UNRESOLVED)
    } else {
      p.setResolutionState(ResolutionState.RESOLVED)
    }
    p.getDependencies.addAll(
      dn.getDependencies.asScala
        .filterNot(d => isSelfProjectModule(d, context.copy(currentModuleName = moduleName)))
        .asJava
    )
    Some(p)
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
      val artifact   = extractArtifactFromName(None, node.getDisplayName)
      val artifactId = artifact.map(_.artifact).getOrElse(Constants.Empty_String)
      val group      = artifact.map(_.group).getOrElse(Constants.Empty_String)
      if (
        context.allModulePaths.keys
          .exists(d => group == context.org && toPlatformModule(artifactId).module == d)
      ) {
        appendChildrenAndFixProjectNodes(
          node,
          node.getDependencies.asScala.toList,
          context
        )
      }
    }
  }

  private def isProjectModule(dn: DependencyNode, context: ModuleContext): Boolean = {
    // module dependency
    val artifactInfo = extractArtifactFromName(Some(dn.getId.toInt), dn.getDisplayName).orNull
    if (artifactInfo == null) return false
    if (artifactInfo.group != context.org) return false

    val matchModule = context.allModulePaths.keys.filter(m => m == toPlatformModule(artifactInfo.artifact).module)

    matchModule.nonEmpty

  }

  /** copy from DependencyModifierService, and fix
   */
  def declaredDependencies(module: OpenapiModule.Module): java.util.List[DeclaredDependency] = try {

    // Check whether the IDE is in Dumb Mode. If it is, return empty list instead proceeding
//    if (DumbService.getInstance(module.getProject).isDumb) return Collections.emptyList()

    val libDeps = SbtDependencyUtils
      .getLibraryDependenciesOrPlaces(getSbtFileOpt(module), module.getProject, module, GetDep)
      .map(_.asInstanceOf[(ScInfixExpr, String, ScInfixExpr)])

    implicit val project: Project = module.getProject

    val scalaVer = SbtDependencyUtils.getScalaVerFromModule(module)

    inReadAction({
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
              new DeclaredDependency(
                new UnifiedDependency(
                  libDepArr(0),
                  libDepArr(1),
                  scope, // if version is a val, not a string
                  scope
                ),
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
