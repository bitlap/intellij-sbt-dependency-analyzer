import org.jetbrains.sbtidea.Keys.*
import org.jetbrains.sbtidea.verifier.FailureLevel

lazy val scala3Version         = "3.7.2"
lazy val logbackVersion        = "1.5.18"
lazy val graphvizVersion       = "0.18.1"
lazy val joorVersion           = "0.9.15"
lazy val scalatestVersion      = "3.2.19"
lazy val pluginVerifierVersion = "1.388"
lazy val ktVersion             = "2.1.0"
lazy val jbAnnotVersion        = "26.0.2"

// https://youtrack.jetbrains.com/articles/IDEA-A-2100661679/IntelliJ-IDEA-2023.3-Latest-Builds
// NOTE: Latest-Builds 233
lazy val intellijVersion = "251.26927.53"
lazy val pluginVersion   = s"0.7.1-$intellijVersion"

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
    scalacOptions ++= Seq(
      "-deprecation",
//      "-Xfatal-warnings",
      "-Ybackend-parallelism:16" // https://github.com/scala/scala3/pull/15392
//       "-nowarn", // during migration
//      "-rewrite",
//      "-source:3.7-migration"
    ),
    version                        := (ThisBuild / version).value,
    ThisBuild / intellijPluginName := "Sbt Dependency Analyzer",
    ThisBuild / intellijBuild      := intellijVersion,
    ThisBuild / intellijPlatform   := (Global / intellijPlatform).??(IntelliJPlatform.IdeaCommunity).value,
    signPluginOptions := signPluginOptions.value.copy(
      enabled = true,
      certFile = Some(file(sys.env.getOrElse("PLUGIN_SIGN_KEY", "/Users/liguobin/chain.crt"))),
      privateKeyFile = Some(file(sys.env.getOrElse("PLUGIN_SIGN_CERT", "/Users/liguobin/private.pem"))),
      keyPassphrase = Some(sys.env.getOrElse("PLUGIN_SIGN_KEY_PWD", "123456"))
    ),
    pluginVerifierOptions := pluginVerifierOptions.value.copy(
      version = pluginVerifierVersion, // use a specific verifier version
      offline = true,                  // forbid the verifier from reaching the internet
      failureLevels = Set(FailureLevel.COMPATIBILITY_PROBLEMS, FailureLevel.COMPATIBILITY_WARNINGS)
    ),
    ThisBuild / bundleScalaLibrary             := true,
    ThisBuild / autoRemoveOldCachedIntelliJSDK := true,
    Global / intellijAttachSources             := true,
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
      "org.scalatest" %% "scalatest"              % scalatestVersion % Test,
      "org.jetbrains"  % "annotations"            % jbAnnotVersion
    ),
    kotlinVersion := ktVersion,
    Compile / unmanagedSourceDirectories += baseDirectory.value / "src" / "main" / "kotlin",
    packageLibraryMappings ++= Seq(
      "org.jetbrains.kotlin"   % ".*"       % ".*" -> None,
      "org.jetbrains"          % ".*"       % ".*" -> None,
      "org.scala-lang"         % "scala-.*" % ".*" -> None,
      "org.scala-lang.modules" % "scala-.*" % ".*" -> None
    )
  )
