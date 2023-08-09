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
- Support since Intellij IDEA 231 (231.9392.1)

**NOTE**: ***The plugin depends on `addDependencyTreePlugin` in `plugins.sbt` file.***

## How to start

If both the Gradle and SBT plugins are enabled in the environment, two analysis buttons will appear. Please try the latter one. (Generally speaking)

**Entry point one**

![](./docs/gotoAnalyze1.png)

**Entry point two**

![](./docs/gotoAnalyze2.png)

> The plugin currently has no depth limit, pay attention to memory.

## Preview

### Show External Dependencies

![](./docs/dependencyTreeSingleModule.png)

### Show Declared Dependencies

If the module has declared dependencies and the number of nodes on the first and second layers is greater than 100, we will only show the declared dependencies in the first layer, which will greatly reduce the number of root nodes.

For example: 

There two declared dependencies in `rolls-zio`:
```scala
    libraryDependencies ++= Seq(
      "dev.zio"       %% "zio"       % zioVersion       % Provided,
      "org.scalatest" %% "scalatest" % scalatestVersion % Test
    )
```

Dependency Tree shows this (There are only 2 root nodes here): 

![](./docs/dependencyTreeMultipleModules.png)

> This project does not have 100 dependencies and is only used to showcase the effect.

### Show Conflicts

![](./docs/dependencyTreeConflicts.png)