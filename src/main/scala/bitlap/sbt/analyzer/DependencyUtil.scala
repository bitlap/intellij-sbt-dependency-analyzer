package bitlap.sbt.analyzer

import scala.jdk.CollectionConverters.*

import com.intellij.buildsystem.model.DeclaredDependency
import com.intellij.buildsystem.model.unified.UnifiedCoordinates
import com.intellij.externalSystem.DependencyModifierService
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/7
 */
object DependencyUtil {

  def getDeclaredDependency(module: Module, project: Project): List[DeclaredDependency] = {
    val dependencyModifierService = DependencyModifierService.getInstance(project)
    dependencyModifierService.declaredDependencies(module).asScala.toList
  }

  def getUnifiedCoordinates(module: Module, project: Project): List[UnifiedCoordinates] = {
    getDeclaredDependency(module, project).map(_.getCoordinates)
  }

}
