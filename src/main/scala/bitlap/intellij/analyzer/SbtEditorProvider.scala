package bitlap.intellij.analyzer

import org.jdom.Element
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.sbt.*

/** @author
 *    梦境迷离
 *  @version 1.0,2023/4/26
 */
final class SbtEditorProvider extends FileEditorProvider with DumbAware:

  override def accept(project: Project, file: VirtualFile): Boolean = {
    if (!SbtUtil.isSbtProject(project)) return false
    val path = file.getPath
    if (!path.endsWith("/" + Sbt.BuildFile)) return false
    val module = ModuleManager.getInstance(project).getModules.toList.find(_.getModuleFile.equals(file))
    if (module.nonEmpty) module.get.getModuleFile.getPath.equals(file.getPath) else false
  }

  override def createEditor(project: Project, file: VirtualFile): FileEditor =
    new UIFormEditor(project, file)

  override def disposeEditor(editor: FileEditor): Unit =
    Disposer.dispose(editor)

  override def readState(
    element: Element,
    project: Project,
    file: VirtualFile
  ): FileEditorState =
    UIFormEditor.MY_EDITOR_STATE

  override def writeState(state: FileEditorState, project: Project, element: Element): Unit = {}

  override def getEditorTypeId = "SbtDependencyAnalyzer"

  override def getPolicy = FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR
