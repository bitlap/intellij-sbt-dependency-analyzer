Sbt Dependency Analyzer for IntelliJ IDEA
---------

<img src="./logo.png" width = "200" height = "100" alt="logo" align="right" />

[![Build](https://github.com/bitlap/intellij-sbt-dependency-analyzer/actions/workflows/ScalaCI.yml/badge.svg)](https://github.com/bitlap/intellij-sbt-dependency-analyzer/actions/workflows/ScalaCI.yml)
[![Version](https://img.shields.io/jetbrains/plugin/v/22427-sbt-dependency-analyzer)](https://plugins.jetbrains.com/plugin/22427-sbt-dependency-analyzer)

## Features

- Same features as the official Gradle Dependency Analyzer
  - View Dependency Tree
  - Show Conflicts
  - Search Dependencies
  - Location Dependency (multi-module)
  - Show dependencies between modules
- Support since Intellij IDEA 231 (231.9392.1)

**NOTE**: ***The plugin depends on `addDependencyTreePlugin` in `plugins.sbt` file.***

## How to start

To use this plugin, it is necessary to ensure that the following preparations are in place:

- In the `project/plugins.sbt` file, there is a statement `addDependencyTreePlugin`. If not, please add it.
- The plugin needs to execute `organization` to obtain the current module `organization`. 
  - The default is to take the last result of the `organization` command as the `groupId`. Therefore, the module must have set `organization`.
  - For multi-module projects, if root module doesn't use `aggregate` to manage sub modules, then each module must be configured with `organization` in order to correctly analyze the dependencies between modules. 
  - To verify if `organization` is correctly configured and you can execute `module/organization` in the sbt shell. By default, unconfigured organization is module name.

**NOTE:** **If both the Gradle and SBT plugins are enabled in the environment, two analysis buttons will appear. Please try the latter one. (Generally speaking)**

**Entry point one**

![](./docs/gotoAnalyze1.png)

**Entry point two**

![](./docs/gotoAnalyze2.png)


### Show Conflicts

![](./docs/dependencyTreeConflicts.png)