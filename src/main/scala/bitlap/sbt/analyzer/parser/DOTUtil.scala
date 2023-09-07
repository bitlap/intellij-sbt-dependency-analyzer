package bitlap
package sbt
package analyzer
package parser

import java.io.File
import java.nio.file.Path

import scala.util.Try

import org.jetbrains.plugins.scala.extensions.inReadAction
import org.jetbrains.plugins.scala.project.VirtualFileExt

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VfsUtil

import guru.nidi.graphviz.attribute.validate.ValidatorEngine
import guru.nidi.graphviz.model.MutableGraph
import guru.nidi.graphviz.parse.Parser
import model.ModuleContext
import util.Notifications

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/3
 */
object DOTUtil {

  private val LOG = Logger.getInstance(classOf[DOTUtil.type])

  private lazy val parser = (new Parser).forEngine(ValidatorEngine.DOT).notValidating()

  private def parseAsGraphTestOnly(file: String): MutableGraph = {
    Try(parser.read(new File(file))).getOrElse(null)

  }

  def parseAsGraph(context: ModuleContext): MutableGraph = {
    if (context.isTest) return parseAsGraphTestOnly(context.analysisFile)
    val file    = context.analysisFile
    var vfsFile = VfsUtil.findFile(Path.of(file), true)
    try {

      val start = System.currentTimeMillis()
      // TODO Tried all kinds of refreshes but nothing works.
      while (vfsFile == null) {
        vfsFile = VfsUtil.findFile(Path.of(file), true)
        if (vfsFile != null) {
          VfsUtil.markDirtyAndRefresh(false, false, false, vfsFile)
        } else {
          if (System.currentTimeMillis() - start > Constants.Timeout.toMillis) {
            LOG.error(s"Cannot get dot file: $file")
            Notifications.notifyParseFileError(file)
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
        Notifications.notifyParseFileError(file)
        LOG.error(s"Cannot parse dot file: $file", e)
        null
    }
  }
}
