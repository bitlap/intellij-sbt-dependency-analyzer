package bitlap.sbt.analyzer.parser

import javax.swing.Icon

import bitlap.sbt.analyzer.SbtDependencyAnalyzerIcons

import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.IconLoader

object DotIcons {
  val FILE: Icon = IconLoader.getIcon("/icons/dot.png", SbtDependencyAnalyzerIcons.getClass)
}

object DotLanguage {
  val INSTANCE = new DotLanguage
}

final class DotLanguage private extends Language("Dot")

final class DotFileType extends LanguageFileType(DotLanguage.INSTANCE) {

  @NotNull def getName: String = "dot file"

  @NotNull def getDescription: String = "dot language file"

  @NotNull def getDefaultExtension: String = "dot"

  @Nullable def getIcon: Icon = DotIcons.FILE

}
