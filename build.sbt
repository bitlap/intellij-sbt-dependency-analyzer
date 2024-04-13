import org.jetbrains.sbtidea.Keys.*
import org.jetbrains.sbtidea.verifier.FailureLevel

lazy val scala3Version         = "3.3.3"
lazy val logbackVersion        = "1.5.5"
lazy val graphvizVersion       = "0.18.1"
lazy val joorVersion           = "0.9.15"
lazy val scalatestVersion      = "3.2.18"
lazy val pluginVerifierVersion = "1.305"
lazy val ktVersion             = "1.9.10"

// https://youtrack.jetbrains.com/articles/IDEA-A-2100661679/IntelliJ-IDEA-2023.3-Latest-Builds
// NOTE: Latest-Builds 233
lazy val intellijVersion = "233.11799.241"
lazy val pluginVersion   = s"0.3.1-$intellijVersion"

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
    scalaVersion := scala3Version,
    organization := "org.bitlap",
    scalacOptions ++= Seq("-deprecation", "-Xfatal-warnings"),
    version                        := (ThisBuild / version).value,
    ThisBuild / intellijPluginName := "Sbt Dependency Analyzer",
    ThisBuild / intellijBuild      := intellijVersion,
    ThisBuild / intellijPlatform   := (Global / intellijPlatform).??(IntelliJPlatform.IdeaCommunity).value,
    signPluginOptions := signPluginOptions.value.copy(
      enabled = true,
      certFile = Some(file("/Users/liguobin/chain.crt")),        // or via PLUGIN_SIGN_KEY env var
      privateKeyFile = Some(file("/Users/liguobin/private.pem")) // or via PLUGIN_SIGN_CERT env var
//      keyPassphrase =
//        Some("xxx") // or None if password is not set(or via PLUGIN_SIGN_KEY_PWD env var)
    ),
    pluginVerifierOptions := pluginVerifierOptions.value.copy(
      version = pluginVerifierVersion, // use a specific verifier version
      offline = true,                  // forbid the verifier from reaching the internet
      failureLevels = Set(FailureLevel.COMPATIBILITY_PROBLEMS, FailureLevel.COMPATIBILITY_WARNINGS)
    ),
    Global / intellijAttachSources := true,
    intellijPlugins ++= Seq("com.intellij.java", "com.intellij.java-i18n", "org.intellij.scala").map(_.toPlugin),
    Compile / unmanagedResourceDirectories += baseDirectory.value / "src" / "main" / "resources",
    Test / unmanagedResourceDirectories += baseDirectory.value / "src" / "test" / "resources",
    patchPluginXml := pluginXmlOptions { xml =>
      xml.version = pluginVersion
//      xml.pluginDescription = IO.read(baseDirectory.value / "src" / "main" / "resources" / "patch" / "description.html")
//      xml.changeNotes = IO.read(baseDirectory.value / "src" / "main" / "resources" / "patch" / "change.html")
    },
    publish / skip := true,
    commands ++= Commands.value,
    libraryDependencies ++= Seq(
      "guru.nidi"      % "graphviz-java-min-deps" % graphvizVersion,
      "ch.qos.logback" % "logback-classic"        % logbackVersion,
      "org.jooq"       % "joor"                   % joorVersion,
      "org.scalatest" %% "scalatest"              % scalatestVersion % Test
    ),
    kotlinVersion := ktVersion,
    Compile / unmanagedSourceDirectories += baseDirectory.value / "src" / "main" / "kotlin",
    packageLibraryMappings ++= Seq(
      "org.jetbrains.kotlin" % ".*" % ".*" -> None,
      "org.jetbrains"        % ".*" % ".*" -> None
    )
  )
