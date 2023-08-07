package bitlap.sbt.analyzer

import java.util.Collections

import scala.jdk.CollectionConverters.*

import bitlap.sbt.analyzer.model.*

import org.jetbrains.plugins.scala.extensions.*
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScInfixExpr
import org.jetbrains.sbt.language.utils.{ SbtDependencyCommon, SbtDependencyUtils }
import org.jetbrains.sbt.language.utils.SbtDependencyCommon.defaultLibScope
import org.jetbrains.sbt.language.utils.SbtDependencyUtils.*
import org.jetbrains.sbt.language.utils.SbtDependencyUtils.GetMode.GetDep

import com.intellij.buildsystem.model.DeclaredDependency
import com.intellij.buildsystem.model.unified.{ UnifiedCoordinates, UnifiedDependency }
import com.intellij.externalSystem.DependencyModifierService
import com.intellij.openapi.actionSystem.{ CommonDataKeys, DataContext }
import com.intellij.openapi.diagnostic.{ ControlFlowException, Logger }
import com.intellij.openapi.externalSystem.model.project.dependencies.*
import com.intellij.openapi.module.Module
import com.intellij.openapi.module as OpenapiModule
import com.intellij.openapi.project.{ DumbService, Project }

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

  def scalaMajorVersion(module: Module) = {
    val scalaVer = SbtDependencyUtils.getScalaVerFromModule(module)
    scalaVer.split("\\.").headOption.getOrElse("3")
  }

  /** ignore self dependency
   */
  def filterModuleSelfDependency(dn: DependencyNode, context: ModuleContext): Boolean = {
    dn.getDisplayName match
      case ArtifactRegex(group, artifact, version) =>
        // TODO exact matching with group
        artifact == context.moduleName + "_" + context.scalaMajor
        || artifact == context.moduleName
      case _ => false
  }

  /** ignore topLevel declared Dependencies
   */
  def filterDeclaredDependency(
    dn: DependencyNode,
    scalaMajor: String,
    declared: List[UnifiedCoordinates]
  ): Boolean = {
    dn.getDisplayName match
      case ArtifactRegex(group, artifact, version) =>
        !declared.exists(uc =>
          group == uc.getGroupId && artifact == uc.getArtifactId + "_" + scalaMajor
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
