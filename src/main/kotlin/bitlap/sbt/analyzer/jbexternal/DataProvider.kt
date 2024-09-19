package bitlap.sbt.analyzer.jbexternal

import com.intellij.openapi.actionSystem.DataProvider
import org.jetbrains.annotations.NonNls

fun interface DataProvider : DataProvider {

    override fun getData(@NonNls dataId: String): Any? = getAnalyzerData(dataId)

    /**
     * In order to meet the IJ standard, we do not directly use the getData function.
     */
    fun getAnalyzerData(@NonNls dataId: String): Any?
}
