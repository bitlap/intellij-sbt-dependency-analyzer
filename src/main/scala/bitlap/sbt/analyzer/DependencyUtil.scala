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

  private val LOG = Logger.getInstance(classOf[DependencyUtil.type])

  def getDeclaredDependency(module: Module, project: Project): List[DeclaredDependency] = {
    declaredDependencies(module).asScala.toList
  }

  def getUnifiedCoordinates(module: Module, project: Project): List[UnifiedCoordinates] = {
    getDeclaredDependency(module, project).map(_.getCoordinates)
  }

  def scalaMajorVersion(module: Module): String = {
    val scalaVer      = SbtDependencyUtils.getScalaVerFromModule(module)
    val scalaVerRegex = "(.*)(\\.)(.*)(\\.)(.*)".r
    scalaVer match
      case scalaVerRegex(major, dot1, minor, dot2, fix) if major == "2" => s"$major.$minor"
      case _                                                            => "3"
  }

  /** ignore self dependency
   *
   *  self is a ProjectDependencyNodeImpl, because we first convert it to DependencyNode and then filter it.
   */
  def filterSelfModuleDependency(dn: DependencyNode, context: ModuleContext): Boolean = {
    dn.getDisplayName match
      case ArtifactRegex(group, artifact, version) =>
        context.org == group && (artifact == context.currentModuleName ++ "_" + context.scalaMajor)
      case _ => false
  }

  def artifactAsName(artifact: ArtifactInfo): String = {
    s"${artifact.group}:${artifact.artifact}:${artifact.version}"
  }

  def extractArtifactFromName(idOpt: Option[Int], name: String): Option[ArtifactInfo] = {
    name match
      case ArtifactRegex(group, artifact, version) =>
        Some(ArtifactInfo(idOpt.getOrElse(id.getAndIncrement()), group, artifact, version))
      case _ => None
  }

  def toProjectDependencyNode(dn: DependencyNode, context: ModuleContext): Option[DependencyNode] = {
    val artifact = extractArtifactFromName(Some(dn.getId.toInt), dn.getDisplayName).orNull
    if (artifact == null) return None
    val moduleName = artifact.artifact.split("_")(0)

    val p = new ProjectDependencyNodeImpl(
      dn.getId,
      moduleName,
      context.allModulePaths.getOrElse(moduleName, "")
    )
    if (p.getProjectPath.isEmpty) {
      p.setResolutionState(ResolutionState.UNRESOLVED)
    } else {
      p.setResolutionState(ResolutionState.RESOLVED)
    }
    p.getDependencies.addAll(dn.getDependencies)
    Some(p)
  }

  def fixProjectModuleDependencies(
    root: DependencyScopeNode,
    nodes: Seq[DependencyNode],
    context: ModuleContext
  ): DependencyScopeNode = {
    root.getDependencies.addAll(nodes.asJava)
    val moduleDependencies = nodes.filter(d => filterModuleDependency(d, context))
    root.getDependencies.removeIf(node => moduleDependencies.exists(_.getId == node.getId))
    val mds = moduleDependencies.map(d => toProjectDependencyNode(d, context)).collect { case Some(value) =>
      value
    }
    root.getDependencies.addAll(mds.asJava)
    root
  }

  def filterModuleDependency(dn: DependencyNode, context: ModuleContext): Boolean = {
    // module dependency
    val artifact = extractArtifactFromName(Some(dn.getId.toInt), dn.getDisplayName).orNull
    if (artifact == null) return false
    if (artifact.group != context.org) return false

    val matchModule = context.allModulePaths.keys.filter(m => (m ++ "_" + context.scalaMajor) == artifact.artifact)

    matchModule.nonEmpty

  }

  /** ignore topLevel declared Dependencies
   */
  def filterNotDeclaredDependency(
    dn: DependencyNode,
    scalaMajor: String,
    declared: List[UnifiedCoordinates]
  ): Boolean = {
    dn.getDisplayName match
      case ArtifactRegex(group, artifact, version) =>
        !declared.exists(uc =>
          group == uc.getGroupId && artifact == uc.getArtifactId ++ "_" + scalaMajor
          || (group == uc.getGroupId && artifact == uc.getArtifactId)
        )
      case _ => false
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
