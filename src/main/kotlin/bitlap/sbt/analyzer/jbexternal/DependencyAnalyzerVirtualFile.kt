// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package bitlap.sbt.analyzer.jbexternal

import com.intellij.icons.AllIcons
import com.intellij.ide.actions.SplitAction
import com.intellij.ide.plugins.UIComponentFileEditor
import com.intellij.ide.plugins.UIComponentVirtualFile
import com.intellij.ide.plugins.UIComponentVirtualFile.Content
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerView
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle
import com.intellij.openapi.project.Project
import com.intellij.util.containers.DisposableWrapperList

/**
 * https://github.com/JetBrains/intellij-community/blob/idea/233.11799.300/platform/external-system-impl/src/com/intellij/openapi/externalSystem/dependency/analyzer/DependencyAnalyzerVirtualFile.kt
 */
internal class DependencyAnalyzerVirtualFile(private val project: Project, private val systemId: ProjectSystemId) :
    UIComponentVirtualFile(
        ExternalSystemBundle.message("external.system.dependency.analyzer.editor.tab.name"),
        AllIcons.Actions.DependencyAnalyzer
    ) {
    private val views = DisposableWrapperList<DependencyAnalyzerView>()

    fun getViews(): List<DependencyAnalyzerView> = views.toList()

    override fun createContent(editor: UIComponentFileEditor): Content {
        val view = DependencyAnalyzerViewImpl(project, systemId, editor)
        views.add(view, editor)
        return Content { view.createComponent() }
    }

    init {
        putUserData(SplitAction.FORBID_TAB_SPLIT, true)
    }
}