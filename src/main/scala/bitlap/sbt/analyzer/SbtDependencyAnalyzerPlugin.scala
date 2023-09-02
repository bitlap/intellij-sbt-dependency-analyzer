package bitlap.sbt.analyzer

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId

/** @author
 *    梦境迷离
 *  @version 1.0,2023/9/1
 */
object SbtDependencyAnalyzerPlugin {

  val PLUGIN_ID = "org.bitlap.sbtDependencyAnalyzer"

  val descriptor: IdeaPluginDescriptor = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID))

  lazy val version: String = descriptor.getVersion
}
