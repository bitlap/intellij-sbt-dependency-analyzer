package bitlap.sbt.analyzer.jbexternal

import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerDependency as Dependency

data class SbtDAArtifact(
    override val groupId: String,
    override val artifactId: String,
    override val version: String,
    val size: Long,
    val totalSize:Long
) : UserDataHolderBase(), Dependency.Data.Artifact {
    override fun toString() = "$groupId:$artifactId:$version"
}