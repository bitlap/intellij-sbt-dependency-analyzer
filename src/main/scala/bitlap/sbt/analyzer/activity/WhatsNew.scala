package bitlap.sbt.analyzer.activity

import bitlap.sbt.analyzer.SbtDependencyAnalyzerBundle

import org.jetbrains.plugins.scala.project.Version

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.impl.HTMLEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Conditions
import com.intellij.ui.jcef.JBCefApp

/** @author
 *    梦境迷离
 *  @version 1.0,2023/9/2
 */
object WhatsNew:
  private lazy val Log     = Logger.getInstance(classOf[WhatsNew.type])
  private val ReleaseNotes = "https://github.com/bitlap/intellij-sbt-dependency-analyzer/releases/tag/v"

  def canBrowseInHTMLEditor: Boolean = JBCefApp.isSupported

  def getReleaseNotes(version: Version): String = ReleaseNotes + version.presentation

  def browse(version: Version, project: Project): Unit = {
    val url = ReleaseNotes + version.presentation
    if (project != null && canBrowseInHTMLEditor) {
      ApplicationManager.getApplication.invokeLater(
        () => {
          try {
            HTMLEditorProvider.openEditor(
              project,
              SbtDependencyAnalyzerBundle
                .message("analyzer.action.whatsNew.text", "Sbt Dependency Analyzer"),
              url,
              // language=HTML
              s"""<div style="text-align: center;padding-top: 3rem">
                 |<div style="padding-top: 1rem; margin-bottom: 0.8rem;">${SbtDependencyAnalyzerBundle.message(
                  "analyzer.notification.updated.failure.title"
                )}</div>
                 |<div><a href="$url" target="_blank"
                 |        style="font-size: 2rem">${SbtDependencyAnalyzerBundle.message(
                  "analyzer.notification.updated.failure.text"
                )}</a></div>
                 |</div>""".stripMargin
            )
          } catch {
            case e: Throwable =>
              Log.warn("""Failed to load "What's New" page""", e)
              BrowserUtil.browse(url)
          }
        },
        ModalityState.NON_MODAL,
        Conditions.is(project.getDisposed)
      )
    } else {
      BrowserUtil.browse(url)
    }
  }

end WhatsNew
