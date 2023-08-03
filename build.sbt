import org.jetbrains.sbtidea.Keys.*

lazy val scala3Version = "3.3.0"

lazy val intellijVersion =
  "231.9392.1" // https://youtrack.jetbrains.com/articles/IDEA-A-2100661425/IntelliJ-IDEA-2023.1-Latest-Builds
lazy val pluginVersion = s"0.1.0-$intellijVersion-SNAPSHOT"

ThisBuild / version := pluginVersion

inThisBuild(
  List(
    homepage := Some(url("https://github.com/bitlap/intellij-sbt-dependency-analyzer")),
    developers := List(
      Developer(
        id = "jxnu-liguobin",
        name = "梦境迷离",
        email = "dreamylost@outlook.com",
        url = url("https://blog.dreamylost.cn")
      ),
      Developer(
        id = "IceMimosa",
        name = "IceMimosa",
        email = "chk19940609@gmail.com",
        url = url("http://patamon.me")
      )
    )
  )
)

lazy val `sbt-dependency-analyzer` = (project in file("."))
  .enablePlugins(SbtIdeaPlugin)
  .settings(
    scalaVersion                   := scala3Version,
    organization                   := "org.bitlap",
    version                        := (ThisBuild / version).value,
    ThisBuild / intellijPluginName := "Sbt Dependency Analyzer",
    ThisBuild / intellijBuild      := intellijVersion,
    ThisBuild / intellijPlatform   := (Global / intellijPlatform).??(IntelliJPlatform.IdeaCommunity).value,
    Global / intellijAttachSources := true,
    intellijPlugins ++= Seq("com.intellij.java", "com.intellij.java-i18n", "org.intellij.scala").map(_.toPlugin),
    Compile / unmanagedResourceDirectories += baseDirectory.value / "src" / "main" / "resources",
    Test / unmanagedResourceDirectories += baseDirectory.value / "src" / "test" / "resources",
    patchPluginXml := pluginXmlOptions { xml =>
      xml.version = pluginVersion
      xml.pluginDescription = IO.read(baseDirectory.value / "src" / "main" / "resources" / "patch" / "description.html")
      xml.changeNotes = IO.read(baseDirectory.value / "src" / "main" / "resources" / "patch" / "change.html")
    },
    publish / skip := true,
    commands ++= Commands.value,
    libraryDependencies ++= Seq(
      // FIXME 0.15.1+ Caused by: java.lang.LinkageError: loader constraint violation: when resolving method 'org.slf4j.ILoggerFactory
      "guru.nidi"      % "graphviz-java" % "0.15.0",
      "io.circe"      %% "circe-core"    % "0.14.3",
      "io.circe"      %% "circe-parser"  % "0.14.3",
      "io.circe"      %% "circe-generic" % "0.14.3",
      "org.scalatest" %% "scalatest"     % "3.2.16" % Test
    )
  )
