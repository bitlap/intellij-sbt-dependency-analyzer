Sbt Dependency Analyzer for IntelliJ IDEA
---------

<img src="./logo.png" width = "200" height = "100" alt="logo" align="right" />

[![Build](https://github.com/bitlap/intellij-sbt-dependency-analyzer/actions/workflows/ScalaCI.yml/badge.svg)](https://github.com/bitlap/intellij-sbt-dependency-analyzer/actions/workflows/ScalaCI.yml)
[![Version](https://img.shields.io/jetbrains/plugin/v/22427-sbt-dependency-analyzer?label=version)](https://plugins.jetbrains.com/plugin/22427-sbt-dependency-analyzer)

## Features

- Same features as the official Gradle Dependency Analyzer
  - View Dependency Tree
  - Show Conflicts
  - Search Dependencies
  - Location Dependency (multi-module)
  - Show dependencies between modules
- Support since Intellij IDEA 2023.1 (231.9392.1)

## How to start

To use this plugin, it is necessary to ensure that the following preparations are in place:

1. In the `project/plugins.sbt` file, there is a statement `addDependencyTreePlugin`. If not, please add it.
2. The plugin needs to execute `organization` to obtain the current module `organization`. 
3. The plugin needs to execute `moduleName` to obtain the all sbt modules.
4. The plugin needs to execute `dependencyDot` to obtain the all dependency trees.

_**NOTE**_:
- The plugin depends on `addDependencyTreePlugin` in `plugins.sbt` file.
- If the SBT version is lower than 1.4, there is no `addDependencyTreePlugin`. In this case, you need to manually add `addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")`.

For more details:
<details>
<summary>Why does it need to use these commands?</summary>
1. The plugin will take the last result of the `organization` command as the `groupId`. Therefore, the module must have set `organization`.</br>
2. For multi-module projects, if root module doesn't use `ThisBuild` or `inThisBuild` to set `organization`, then each module must be configured with `organization` in order to correctly analyze the dependencies between modules (such as: module A `dependsOn` module B).</br>
3. To verify if `organization` is correctly configured, you can execute `organization` in the sbt shell. If not configured, the `organization` is a module name, which will not be able to analyze the modules that the current module depends on.</br>
4. The plugin will take the sbt module name to check `artifactId` in dependency trees.</br> 
</details>


_**NOTE**_:
- If both the Gradle and SBT plugins are enabled in the environment, two analysis buttons will appear. Please try the latter one. (Generally speaking, this is likely an issue with the Intellij IDEA or Intellij gradle plugin)

<details>
<summary>Entry point one 👈🏻</summary>

![](./docs/gotoAnalyze1.png)

</details>

<details>
<summary>Entry point two 👈🏻</summary>

![](./docs/gotoAnalyze2.png)

</details>


<details>
<summary>Show Conflicts 👈🏻</summary>

![](./docs/scalaJSDependencyTree.png)

</details>