package bitlap.intellij.analyzer

import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.PropertyKey

import com.intellij.DynamicBundle

import SbtPluginBundle.*

final class SbtPluginBundle extends DynamicBundle(BUNDLE)

object SbtPluginBundle {

  final val BUNDLE   = "messages.SbtPluginBundle"
  final val INSTANCE = new SbtPluginBundle

  @Nls def message(@NotNull @PropertyKey(resourceBundle = BUNDLE) key: String, @NotNull params: AnyRef*): String =
    INSTANCE.getMessage(key, params)
}
