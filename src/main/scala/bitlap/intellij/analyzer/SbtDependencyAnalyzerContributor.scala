package bitlap.intellij.analyzer

import com.intellij.openapi.project.Project
import com.intellij.openapi.externalSystem.dependency.analyzer.*
import com.intellij.openapi.Disposable
import kotlin.jvm.functions
import com.intellij.openapi.util.Key
import com.intellij.openapi.externalSystem.model.project.ModuleData

import java.util

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/1
 */
final class SbtDependencyAnalyzerContributor(project: Project) extends DependencyAnalyzerContributor {
  override def getDependencies(
    dependencyAnalyzerProject: DependencyAnalyzerProject
  ): util.List[DependencyAnalyzerDependency] = ???

  override def getDependencyScopes(
    dependencyAnalyzerProject: DependencyAnalyzerProject
  ): util.List[DependencyAnalyzerDependency.Scope] = ???

  override def getProjects: util.List[DependencyAnalyzerProject] = ???

  override def whenDataChanged(function0: functions.Function0[kotlin.Unit], disposable: Disposable): Unit = ???
}
object SbtDependencyAnalyzerContributor {
  val MODULE_DATA = Key.create[ModuleData]("SbtDependencyAnalyzerContributor.ModuleData")

}
