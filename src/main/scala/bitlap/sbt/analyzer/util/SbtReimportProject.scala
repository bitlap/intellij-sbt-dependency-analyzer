package bitlap.sbt.analyzer.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic

object SbtReimportProject {

  val _Topic: Topic[ReimportProjectListener] =
    Topic.create("SbtDependencyAnalyzerReimportProject", classOf[ReimportProjectListener])

  trait ReimportProjectListener:

    def onReimportProject(project: Project): Unit

  end ReimportProjectListener

  val ReimportProjectPublisher: ReimportProjectListener =
    ApplicationManager.getApplication.getMessageBus.syncPublisher(_Topic)

}
