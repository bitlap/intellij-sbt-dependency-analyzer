Sbt Dependency Analyzer for IntelliJ IDEA
---------

<img src="./logo.svg" width = "250" height = "150" alt="logo" align="right" />

[![Project stage](https://img.shields.io/badge/Project%20Stage-Production%20Ready-brightgreen.svg)](https://github.com/bitlap/bitlap/wiki/Project-Stages)
[![Build](https://github.com/bitlap/intellij-sbt-dependency-analyzer/actions/workflows/ScalaCI.yml/badge.svg)](https://github.com/bitlap/intellij-sbt-dependency-analyzer/actions/workflows/ScalaCI.yml)
[![Version](https://img.shields.io/jetbrains/plugin/v/22427-sbt-dependency-analyzer?label=Version)](https://plugins.jetbrains.com/plugin/22427-sbt-dependency-analyzer/versions)
[![JetBrains Plugin Downloads](https://img.shields.io/jetbrains/plugin/d/22427?label=JetBrains%20Plugin%20Downloads)](https://plugins.jetbrains.com/plugin/22427-sbt-dependency-analyzer)

English | [中文](README-CN.md)

**If you find the Sbt Dependency Analyzer interesting, please ⭐ [Star](https://github.com/bitlap/intellij-sbt-dependency-analyzer) it at the top of the GitHub page to support us.**

## Features

> Support available since IntelliJ IDEA 2023.1 (231.9392.1)

- [x] View Dependency Tree
- [x] Show Conflicts
- [x] Search Dependencies
- [x] Goto Dependency (Jump to the location defined by the dependency)
- [x] Show Dependencies Between Modules
- [x] Show JAR Size
- [x] Dependency Exclusion (Experimental) 
  - **Selecting transitive dependencies in user-defined dependencies indicates exclusion, while selecting user-defined dependencies indicates deletion itself**
  - Available since Sbt Dependency Analyzer `0.5.0-242.21829.142`

## Usage Instructions

This plugin will automatically generate `project/sdap.sbt` when the first analysis fails and insert the `addDependencyTreePlugin` (or `addSbtPlugin(...)`) statement into it. If generated, please do not modify or delete `project/sdap.sbt`. 

This plugin relies on `sbt-dependency-tree`, a third-party plugin, which is now integrated into sbt by default (although it won't be enabled by default, as explained in this [sbt issue](https://github.com/sbt/sbt/pull/5880)).

**Let's explore how to use it!**

> Default shortcut: Ctrl + Shift + L

![image](https://plugins.jetbrains.com/files/22427/screenshot_064531dc-a3fa-4a8e-9437-7e76defa1f48)

## More Details

The plugin utilizes the following sbt commands. However, rest assured that the plugin has been optimized to minimize the number of executions as much as possible: `organization`,`moduleName`,`dependencyDot`,`reload`,`update`

## Advanced Setup

> If you are uncertain, you can safely skip these configurations!

By utilizing configurations, analysis wait times can be significantly reduced:

<img src="./docs/settings.png" width = "400" height = "280" alt="settings" align="right" />

**File Cache Timeout**

If the dependent file (`.dot`) has not been modified within the last `3600 seconds` (default value), the plugin will continue to use the existing file for analysis, 
otherwise the `dependencyDot` command will be executed, which is a certain degree of caching, but the caching may not take effect when the project first opens the analysis graph.

**Organization** 

If you specify this value, the `organization` command will not be used to retrieve your project's organization.

**Disable Scopes**

If you do not need to analyze all scopes, simply disable the scope(s) you wish to skip.

Configurations are persistent and associated with each IntelliJ project.

Like other plugins, this one maintains its own storage in `.idea/bitlap.sbt.dependency.analyzer.xml`. Deleting this file will clear the cache.

## Troubleshooting Issues

### "Caused by: java.io.IOException: Could not create lock for ..."

Due to the plugin's requirement to use sbt shell, opening the dependency analysis view and subsequently using IntelliJ IDEA to reload or build the project may lead to the following issue:
```
Caused by: java.io.IOException: Could not create lock for \\.\pipe\sbt-load5964714308503584069_lock, error 5
```

To avoid this problem, utilize sbt shell for reloading or building the project:

<img src="./docs/sbtShellUseForReload.jpg" width = "500" height = "230" alt="settings" align="center" />

### Unable to analyze dependencies between modules?

Ensure that you have applied one of the following settings to help identify the correct module:
- The `organization` in [Advanced Setup](#advanced-setup) has been configured.
- The `organization` value has been set in `build.sbt` via `ThisBuild` or `inThisBuild`.

> Note: Sub modules that are not in the `dependsOn` of the root project will not be parsed and their dependencies will be empty.

## JetBrains Support

This project is developed using JetBrains IDEA.
Thanks to JetBrains for providing me with a free license, which is a strong support for me.

<a href="www.jetbrains.com">
<img src="https://resources.jetbrains.com/storage/products/company/brand/logos/jb_beam.svg?_gl=1*8f2ovk*_ga*NTY2NTA4Mzg1LjE2NzU3MzgzMTI.*_ga_9J976DJZ68*MTcwMzIwOTE4NS4xODUuMS4xNzAzMjA5NDYzLjI4LjAuMA..&_ga=2.177269094.2105719560.1703209186-566508385.1675738312" alt="IntelliJ IDEA logo.">
</a>

<br />
