package bitlap.sbt.analyzer.util

import java.io.*
import java.net.URI
import java.util.Properties
import java.util.jar.JarFile

import scala.collection.mutable
import scala.jdk.CollectionConverters.MapHasAsScala
import scala.util.Using

import org.jetbrains.sbt.SbtUtil as SSbtUtil
import org.jetbrains.sbt.project.*
import org.jetbrains.sbt.project.settings.*

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.*

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/24
 */
object SbtUtils {

  private val log = Logger.getInstance(getClass)

  def getSbtSettings(dir: String, project: Project): SbtExecutionSettings =
    SbtExternalSystemManager.executionSettingsFor(project, dir)

  def launcherJar(sbtSettings: SbtExecutionSettings): File =
    sbtSettings.customLauncher.getOrElse(SSbtUtil.getDefaultLauncher)

  def getSbtVersion(project: Project): String = {
    val workingDirPath = getWorkingDirPath(project)
    val sbtSettings    = getSbtSettings(workingDirPath, project)
    lazy val launcher  = launcherJar(sbtSettings)
    SSbtUtil.detectSbtVersion(new File(workingDirPath), launcher)
  }

  // see https://github.com/JetBrains/intellij-scala/blob/idea232.x/sbt/sbt-impl/src/org/jetbrains/sbt/shell/SbtProcessManager.scala
  def getWorkingDirPath(project: Project): String = {
    // Fist try to calculate root path based on `getExternalRootProjectPath`
    // When sbt project reference another sbt project via `RootProject` this will correctly find the root project path (see SCL-21143)
    // However, if user manually linked multiple SBT projects via external system tool window (sbt tool window)
    // using "Link sbt Project" button (the one with "plus" icon), it  will randomly choose one of the projects
    val externalRootProjectPath: Option[String] = {
      val modules = ModuleManager.getInstance(project).getModules.toSeq
      modules.iterator.map(ExternalSystemApiUtil.getExternalRootProjectPath).find(_ != null)
    }
    externalRootProjectPath.orElse {
      // Not sure when externalRootProjectPath can be empty in SBT projects
      // But just in case fallback to ProjectUtil.guessProjectDir, but notice that it's not reliable in some cases (see SCL-21143)
      val message =
        s"Can't calculate external root project path for project `${project.getName}`, fallback to `ProjectUtil.guessProjectDir`"
      if (ApplicationManager.getApplication.isInternal)
        log.error(message)
      else
        log.warn(message)
      Option(ProjectUtil.guessProjectDir(project)).map(_.getCanonicalPath)
    }
      .getOrElse(throw new IllegalStateException(s"no project directory found for project ${project.getName}"))
  }

}
