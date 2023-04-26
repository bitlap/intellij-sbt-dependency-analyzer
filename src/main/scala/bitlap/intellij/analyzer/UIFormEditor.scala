package bitlap.intellij.analyzer

import java.beans.PropertyChangeListener
import javax.swing.*
import org.jetbrains.annotations.NotNull
import org.jetbrains.idea.maven.project.MavenProject
import org.jetbrains.idea.maven.project.MavenProjectsManager
import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.sbt.*
import org.jetbrains.sbt.language.utils.{ SbtArtifactInfo, SbtDependencyUtils }
import com.intellij.openapi.module.{ Module, ModuleManager }
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.sbt.language.utils.SbtDependencyUtils.GetMode

/** @author
 *    梦境迷离
 *  @version 1.0,2023/4/26
 */
class UIFormEditor(project: Project, file: VirtualFile) extends UserDataHolderBase, FileEditor:
  // ??
  val module: Module =
    ModuleManager.getInstance(project).getModules.toList.find(_.getModuleFile.getPath.equals(file.getPath)).orNull

  if (project == null || module == null)
    throw new RuntimeException("Report this bug please. Project not found for file " + file.getPath)

  val myEditor = new GuiForm(project, file)

  override def getComponent: JComponent = ???
//    myEditor.getRootComponent

  override def dispose(): Unit = {}

  override def getPreferredFocusedComponent: JComponent = ???
//    myEditor.getPreferredFocusedComponent

  override def getName = "Sbt Dependency Analyzer"

  override def isModified = false

  override def isValid = true

  override def selectNotify(): Unit = ???
//    myEditor.selectNotify()

  override def deselectNotify(): Unit = {}

  override def addPropertyChangeListener(listener: PropertyChangeListener): Unit = {}

  override def removePropertyChangeListener(listener: PropertyChangeListener): Unit = {}

  override def getBackgroundHighlighter: BackgroundEditorHighlighter = null

  override def getCurrentLocation: FileEditorLocation = null

  override def getState(ignored: FileEditorStateLevel): FileEditorState = UIFormEditor.MY_EDITOR_STATE

  override def setState(state: FileEditorState): Unit = {}

  override def getStructureViewBuilder: StructureViewBuilder = null

object UIFormEditor:

  val MY_EDITOR_STATE = new FileEditorState() {
    def canBeMergedWith(otherState: FileEditorState, level: FileEditorStateLevel) = false
  }
