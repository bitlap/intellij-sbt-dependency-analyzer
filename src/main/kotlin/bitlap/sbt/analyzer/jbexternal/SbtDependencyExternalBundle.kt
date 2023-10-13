package bitlap.sbt.analyzer.jbexternal

import com.intellij.DynamicBundle
import org.jetbrains.annotations.*

/**
 *
 * @author 梦境迷离
 * @version 1.0,2023/10/13
 */
class SbtDependencyExternalBundle : DynamicBundle(BUNDLE) {
    companion object {
        const val BUNDLE = "messages.SbtPluginExternalBundle"
        private val INSTANCE = SbtDependencyExternalBundle()

        @Nls
        fun message(@NotNull @PropertyKey(resourceBundle = BUNDLE) key: String, @NotNull vararg params: Any): String =
            INSTANCE.getMessage(key, params)
    }
}