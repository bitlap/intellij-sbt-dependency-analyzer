package bitlap.sbt.analyzer

import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.PropertyKey

import com.intellij.DynamicBundle

import SbtDependencyAnalyzerBundle.*

final class SbtDependencyAnalyzerBundle extends DynamicBundle(BUNDLE)

object SbtDependencyAnalyzerBundle {

  final val BUNDLE   = "messages.SbtDependencyAnalyzerBundle"
  final val INSTANCE = new SbtDependencyAnalyzerBundle

  @Nls def message(@NotNull @PropertyKey(resourceBundle = BUNDLE) key: String, @NotNull params: AnyRef*): String =
    INSTANCE.getMessage(key, params: _*)
}
