package bitlap.intellij.analyzer.configuration

import bitlap.intellij.analyzer.configuration.SbtRuntimeTargetConfiguration.MyState

import com.intellij.execution.target.LanguageRuntimeConfiguration
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.PersistentStateComponent

import kotlin.jvm.internal.*
import kotlin.reflect.KProperty

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/2
 */
final class SbtRuntimeTargetConfiguration
    extends LanguageRuntimeConfiguration(SbtRuntimeType.TYPE_ID),
      PersistentStateComponent[SbtRuntimeTargetConfiguration.MyState] {

  var homePath: String = ""

  override def getState: SbtRuntimeTargetConfiguration.MyState =
    val status = new MyState()
    status.homePath = homePath
    status

  override def loadState(state: SbtRuntimeTargetConfiguration.MyState): Unit = {
    this.homePath = if (state.homePath == null) state.homePath else ""
  }
}

object SbtRuntimeTargetConfiguration {

  class MyState extends BaseState() {

    val var0: KProperty[_] = Reflection
      .mutableProperty1(
        new MutablePropertyReference1Impl(
          classOf[SbtRuntimeTargetConfiguration.MyState],
          "homePath",
          "getHomePath()Ljava/lang/String;",
          0
        ).asInstanceOf[MutablePropertyReference1]
      )
      .asInstanceOf[KProperty[_]]

    var homePath: String = string(null).getValue(this, var0)
  }
}
