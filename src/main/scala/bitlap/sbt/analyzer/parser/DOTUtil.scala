package bitlap.sbt.analyzer.parser

import java.io.File
import java.nio.file.Path

import scala.util.Try

import bitlap.sbt.analyzer.{ Constants, DependencyUtils }
import bitlap.sbt.analyzer.component.SbtDependencyAnalyzerNotifier

import org.jetbrains.plugins.scala.extensions.inReadAction
import org.jetbrains.plugins.scala.project.VirtualFileExt

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.{ LocalFileSystem, VfsUtil }
import com.intellij.openapi.vfs.newvfs.VfsImplUtil

import guru.nidi.graphviz.attribute.validate.ValidatorEngine
import guru.nidi.graphviz.model.MutableGraph
import guru.nidi.graphviz.parse.Parser

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/3
 */
object DOTUtil {

  private val LOG = Logger.getInstance(classOf[DOTUtil.type])

  private lazy val parser = (new Parser).forEngine(ValidatorEngine.DOT).notValidating()

  def parseAsGraph(file: String): MutableGraph = {
    var vfsFile = VfsUtil.findFile(Path.of(file), true)
    try {

      val start = System.currentTimeMillis()
      // TODO Tried all kinds of refreshes but nothing works.
      while (vfsFile == null) {
        vfsFile = VfsUtil.findFile(Path.of(file), true)
        if (vfsFile != null) {
          VfsUtil.markDirtyAndRefresh(false, false, false, vfsFile)
        } else {
          if (System.currentTimeMillis() - start > Constants.timeout.toMillis) {
            LOG.error(s"Cannot get dot file: $file")
            SbtDependencyAnalyzerNotifier.parseFileError(file)
            return null
          }
        }
      }
      inReadAction {
        val f = vfsFile.findDocument.map(_.getImmutableCharSequence.toString).orNull
        parser.read(f)
      }

    } catch {
      case e: Throwable =>
        SbtDependencyAnalyzerNotifier.parseFileError(file)
        LOG.error(s"Cannot parse dot file: $file", e)
        null
    }
  }
}
