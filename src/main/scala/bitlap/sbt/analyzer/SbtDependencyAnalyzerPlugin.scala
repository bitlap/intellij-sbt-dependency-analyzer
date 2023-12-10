package bitlap.sbt.analyzer

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId

object SbtDependencyAnalyzerPlugin {

  val PLUGIN_ID = "org.bitlap.sbtDependencyAnalyzer"

  val descriptor: IdeaPluginDescriptor = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID))

  lazy val version: String = descriptor.getVersion
}
