package bitlap
package sbt
package analyzer
package util

import java.util.Collections
import java.util.concurrent.atomic.AtomicLong

import scala.jdk.CollectionConverters.*

import bitlap.sbt.analyzer.util.SbtDependencyUtils

import org.jetbrains.plugins.scala.ScalaVersion
import org.jetbrains.plugins.scala.extensions.*
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScInfixExpr
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScPatternDefinition
import org.jetbrains.plugins.scala.project.*
import org.jetbrains.sbt.SbtUtil as SSbtUtil
import org.jetbrains.sbt.language.SbtFileImpl
import org.jetbrains.sbt.language.utils.*
import org.jetbrains.sbt.project.SbtProjectSystem

import com.intellij.buildsystem.model.DeclaredDependency
import com.intellij.buildsystem.model.unified.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.diagnostic.*
import com.intellij.openapi.externalSystem.dependency.analyzer.DAScope
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.dependencies.*
import com.intellij.openapi.externalSystem.service.project.nameGenerator.ModuleNameGenerator
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module as OpenapiModule
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiManager

import model.*
import parser.*

object DependencyUtils {

  final val DefaultConfiguration = toDAScope("default")

  private final val rootId     = new AtomicLong(0)
  private final val artifactId = new AtomicLong(0)

  private val SBT_ARTIFACT_REGEX      = "(.*):(.*):(.*)".r
  private val SCALA_ARTIFACT_REGEX    = "(.*)_(.*)".r
  private val NATIVE_ARTIFACT_PATTERN = "_native\\d(\\.\\d+)?_\\d(\\.\\d+)?"
  private val SJS_ARTIFACT_PATTERN    = "_sjs\\d(\\.\\d+)?_\\d(\\.\\d+)?"
  private val NATIVE_ARTIFACT_REGEX   = "(.*)(_native\\d(\\.\\d+)?)_(.*)".r
  private val SJS_ARTIFACT_REGEX      = "(.*)(_sjs\\d(\\.\\d+)?)_(.*)".r

  private final case class PlatformModule(
    module: String,
    scalaVersion: String
  )

  private val LOG: Logger = Logger.getInstance(classOf[DependencyUtils.type])

  def getDeclaredDependency(module: OpenapiModule): List[DeclaredDependency] = {
    declaredDependencies(module).asScala.toList
  }

  /** self is a ProjectDependencyNodeImpl, because we first convert it to DependencyNode and then filter it. This is
   *  important, for dependency graphs/trees, this is the root node.
   */
  def isSelfNode(dn: DependencyNode, context: ModuleContext): Boolean = {
    dn.getDisplayName match
      case SBT_ARTIFACT_REGEX(group, artifact, _) =>
        context.organization == group && isSelfArtifact(artifact, context)
      case _ => false
  }

  def getArtifactInfoFromDisplayName(displayName: String): Option[ArtifactInfo] = {
    displayName match
      case SBT_ARTIFACT_REGEX(group, artifact, version) =>
        Some(ArtifactInfo(artifactId.getAndIncrement().toInt, group, artifact, version))
      case _ => None
  }

  def toDAScope(name: String): DAScope = DAScope(name, StringUtil.toTitleCase(name))

  /** do not analyze this module
   */
  def canIgnoreModule(module: OpenapiModule): Boolean = {
    // if module is itself a build module, skip build module
    val isBuildModule = module.isBuildModule
    isBuildModule || module.isSharedSourceModule
  }

  def getScopedCommandKey(project: String, scope: DependencyScopeEnum, cmd: String): String = {
    if (project == null || project.isEmpty) s"$scope / $cmd"
    else s"$project / $scope / $cmd"
  }

  def analysisFilePath(scope: DependencyScopeEnum, parserTypeEnum: AnalyzedFileType): String =
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
      val artifact   = getArtifactInfoFromDisplayName(node.getDisplayName)
      val artifactId = artifact.map(_.artifact).getOrElse(Constants.EMPTY_STRING)
      val group      = artifact.map(_.group).getOrElse(Constants.EMPTY_STRING)
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
    // processing cross-platform, module name is not artifact!
    val currentModuleName =
      context.ideaModuleIdSbtModuleNames.getOrElse(
        context.currentModuleId,
        context.ideaModuleIdSbtModuleNames.getOrElse(
          Constants.SINGLE_SBT_MODULE,
          context.ideaModuleIdSbtModuleNames.getOrElse(Constants.ROOT_SBT_MODULE, context.currentModuleId)
        )
      )

    // NOTE: we don't determine the Scala version number.
    if (context.isScalaNative) {
      val module = artifact.replaceAll(NATIVE_ARTIFACT_PATTERN, Constants.EMPTY_STRING)
      currentModuleName.equalsIgnoreCase(module)
    } else if (context.isScalaJs) {
      val module = artifact.replaceAll(SJS_ARTIFACT_PATTERN, Constants.EMPTY_STRING)
      currentModuleName.equalsIgnoreCase(module)
    } else {
      artifact match
        case SCALA_ARTIFACT_REGEX(module, _) =>
          currentModuleName.equalsIgnoreCase(module)
        // it is a java project
        case _ => artifact.equalsIgnoreCase(currentModuleName)
    }
  }

  private def toPlatformModule(artifact: String): PlatformModule = {
    artifact match
      case SJS_ARTIFACT_REGEX(module, _, _, scalaVer)    => PlatformModule(module, scalaVer)
      case NATIVE_ARTIFACT_REGEX(module, _, _, scalaVer) => PlatformModule(module, scalaVer)
      case SCALA_ARTIFACT_REGEX(module, scalaVer)        => PlatformModule(module, scalaVer)
      case _                                             => PlatformModule(artifact, Constants.EMPTY_STRING)
  }

  private def toProjectDependencyNode(dn: DependencyNode, context: ModuleContext): Option[DependencyNode] = {
    val artifactInfo = getArtifactInfoFromDisplayName(dn.getDisplayName).orNull
    if (artifactInfo == null) return None
    val sbtModuleName  = toPlatformModule(artifactInfo.artifact).module
    val ideaModuleName = context.ideaModuleIdSbtModuleNames.find(_._2 == sbtModuleName).map(_._1)

    // Processing cross-platform, module name is not artifact
    // This is a project node, we need a module not an artifact to get project path!

    val fixedCustomName = context.ideaModuleNamePaths.map { case (name, path) =>
      if (name.exists(_ == ' '))
        name.toLowerCase.replace(' ', '-') -> path
      else
        name -> path
    }

    val projectPath =
      ideaModuleName
        .flatMap(m => context.ideaModuleNamePaths.get(m))
        .getOrElse(
          context.ideaModuleNamePaths
            .getOrElse(sbtModuleName, fixedCustomName.getOrElse(sbtModuleName.toLowerCase, Constants.EMPTY_STRING))
        )

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
        .filterNot(d => isSelfNode(d, context.copy(currentModuleId = sbtModuleName)))
        .asJava
    )
    Some(p)
  }

  private def isProjectModule(dn: DependencyNode, context: ModuleContext): Boolean = {
    // module dependency
    val artifactInfo = getArtifactInfoFromDisplayName(dn.getDisplayName).orNull
    if (artifactInfo == null) return false
    if (artifactInfo.group != context.organization) return false
    // Use artifact to determine whether there are modules in the dependency.
    val matchModule =
      context.ideaModuleIdSbtModuleNames.values.filter(m => m == toPlatformModule(artifactInfo.artifact).module)

    matchModule.nonEmpty

  }

  /** copy from DependencyModifierService, and fix
   */
  def declaredDependencies(module: OpenapiModule): java.util.List[DeclaredDependency] = try {
    // Check whether the IDE is in Dumb Mode. If it is, return empty list instead proceeding
    // if (DumbService.getInstance(module.getProject).isDumb) return Collections.emptyList()
    val scalaVer = module.scalaMinorVersion.map(_.major).getOrElse(ScalaVersion.default.major)
    inReadAction({
      val libDeps = SbtDependencyUtils
        .getLibraryDependenciesOrPlaces(
          SbtDependencyUtils.getSbtFileOpt(module),
          module.getProject,
          module,
          SbtDependencyUtils.GetMode.GetDep
        )
        .map(_.asInstanceOf[(ScInfixExpr, String, ScInfixExpr)])
      libDeps
        .map(libDepInfixAndString => {
          val libDepArr = SbtDependencyUtils
            .processLibraryDependencyFromExprAndString(libDepInfixAndString) // exist some issues
            .map(_.asInstanceOf[String])
          val dataContext: DataContext = (dataId: String) => {
            if (CommonDataKeys.PSI_ELEMENT.is(dataId)) {
              libDepInfixAndString
            } else null
          }

          libDepArr.length match {
            case x if x < 3 || x > 4 => null
            case x if x >= 3 =>
              val scope = if (x == 3) SbtDependencyCommon.defaultLibScope else libDepArr(3)
              val fixedArtifact =
                if (!DependencyScopeEnum.values.exists(_.toString.toLowerCase.contains(scope.toLowerCase))) {
                  scope
                } else libDepArr(1)
              if (SbtDependencyUtils.isScalaLibraryDependency(libDepInfixAndString._1))
                new DeclaredDependency(
                  new UnifiedDependency(
                    libDepArr.head,
                    SbtDependencyUtils.buildScalaArtifactIdString(libDepArr.head, fixedArtifact, scalaVer),
                    libDepArr(2),
                    scope
                  ),
                  dataContext
                )
              else
                new DeclaredDependency(
                  new UnifiedDependency(libDepArr.head, fixedArtifact, libDepArr(2), scope),
                  dataContext
                )
          }
        })
        .filter(_ != null)
        .toList
        .asJava
    })
  } catch {
    case c: ControlFlowException =>
      throw c
    case e: Exception =>
      LOG.warn(
        s"Error occurs when obtaining the list of dependencies for module ${module.getName} using package search plugin",
        e
      )
      Collections.emptyList()
  }

  def containsModuleName(proj: ScPatternDefinition, module: OpenapiModule): Boolean = {
    val project    = module.getProject
    val moduleName = module.getName
    val settings   = SSbtUtil.sbtSettings(project)
    val moduleData = SSbtUtil.getSbtModuleDataNode(module)
    if (moduleData.isEmpty) {
      return false
    }
    def isEqualModule(name: String) =
      proj.getText.toLowerCase.contains(("\"" + name + "\"").toLowerCase) ||
      proj.getText.toLowerCase.contains(
        ("lazy val `" + name + "`").toLowerCase
      ) ||                                                                  // if project doesn't set module name
      proj.getText.toLowerCase.contains(("val `" + name + "`").toLowerCase) // if project doesn't set module name

    val projectSettings = settings.getLinkedProjectSettings(moduleData.orNull.getData.getLinkedExternalProjectPath)
    val moduleExists    = proj.getText.toLowerCase.contains("\"" + moduleName + "\"".toLowerCase)
    val fixModuleName = if (!projectSettings.isUseQualifiedModuleNames && moduleName.exists(_ == '-')) {
      isEqualModule(moduleName)
    } else {
      // hard code
      if (projectSettings.isUseQualifiedModuleNames && moduleName.exists(_ == '.')) {
        moduleName.count(_ == ' ') match
          case 1 =>
            val mm = moduleName.split(' ').last // root.Circe core.coreJs
            if (mm.count(_ == '.') == 1) {
              if (module.isScalaJs || module.isScalaNative) isEqualModule(mm.split('.').head) // core.coreJs
              else isEqualModule(mm.split('.').last) || isEqualModule(mm.split('.').head)     // zim.zim-api
            } else isEqualModule(mm)

          case i if i > 1 =>
            // root.Circe scalafix internal input
            isEqualModule(moduleName.split(' ').tail.mkString("/")) || isEqualModule(
              moduleName.split(' ').tail.mkString("-")
            ) ||
            // root.Circe numbers testing.numbersTestingJS
            isEqualModule(moduleName.split(' ').last.split('.').head.replace(" ", "-"))
          case 0 =>
            // pekko-root.pekko.actor, pekko-root.pekko-actor
            val splits = moduleName.split('.')
            if (moduleName.exists(_ == '-')) {
              isEqualModule(splits.last) ||
              isEqualModule(splits.last.replace("-", ".")) ||
              isEqualModule(splits.last.split('-').tail.mkString("-")) ||
              isEqualModule(splits.last.split('.').tail.mkString("."))
            } else isEqualModule(moduleName) || isEqualModule(splits.last)
      } else {
        isEqualModule(moduleName)
      }
    }
    moduleExists || fixModuleName
  }
}
