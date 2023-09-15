Sbt Dependency Analyzer for IntelliJ IDEA
---------

<img src="./logo.png" width = "200" height = "100" alt="logo" align="right" />

[![Build](https://github.com/bitlap/intellij-sbt-dependency-analyzer/actions/workflows/ScalaCI.yml/badge.svg)](https://github.com/bitlap/intellij-sbt-dependency-analyzer/actions/workflows/ScalaCI.yml)
[![Version](https://img.shields.io/jetbrains/plugin/v/22427-sbt-dependency-analyzer?label=Version)](https://plugins.jetbrains.com/plugin/22427-sbt-dependency-analyzer)
![JetBrains Plugin Downloads](https://img.shields.io/jetbrains/plugin/d/22427?label=JetBrains%20Plugin%20Downloads)


## Features

> Support since Intellij IDEA 2023.1 (231.9392.1)

- [x] View Dependency Tree
- [x] Show Conflicts
- [x] Search Dependencies
- [x] Locate Dependency (multi-module)
- [x] Show dependencies between modules

## Usage Instructions

This plugin will automatically generate `project/sdap.sbt` and put code `addDependencyTreePlugin` (or `addSbtPlugin(...)`) statement into it, do not modify or delete `project/sdap.sbt`. 

Because this plugin depends on `sbt-dependency-tree` which is a third-party plugin, but now integrated into sbt by default (but the plugin will not be enabled by default, see [sbt issue](https://github.com/sbt/sbt/pull/5880)).

But if statement **already exists** in `*.sbt` files, `sdap.sbt` will not be created.

**Let's see how to use it!**

Just click on the icon and wait for the analysis:

<img src="./docs/gotoAnalyze1.jpg" width = "400" height = "280" alt="settings" align="center" />

When the analysis is complete:

<img src="./docs/dependencyTreeConflicts.jpg" width = "600" height = "300" alt="settings" align="center" />

## More Details

The plugin will use these sbt tasks. But trust me, the plugin has done its best to minimize the need to avoid redundant execution:

1. `organization`
2. `moduleName`
3. `dependencyDot`
4. `reload`
5. `update`

## Advanced Setup

> If you are not sure, you do not need to use these configurations!

Using configurations, analysis wait times can be dramatically reduced:

<img src="./docs/settings.png" width = "400" height = "280" alt="settings" align="right" />

**File Cache Timeout**

If the file hasn't been changed for more than `3600` seconds, plugin will execute the `dependencyDot` task, otherwise use the one that already exists, unless using `Refresh`.

**Organization** 

If you set this value, the `organization` task will not be used to get your project's organization. 

**Disable Scope**

If you don't need to analyze all scopes, just disable it.

Configurations are persistent and associated with each intellij project.

As with other plugins, this plugin has its own storage which is `.idea/bitlap.sbt.dependency.analyzer.xml`, if this file is deleted, the cache will be cleared and the cache of the `moduleName` task will be removed.

## Troubleshooting Issues

### "Caused by: java.io.IOException: Could not create lock for ..."

Due to the need for the plugin to use sbt shell, when you open the dependency analysis view and start using the Intellij IDEA to reload or build project, it may cause this problem:
```
Caused by: java.io.IOException: Could not create lock for \\.\pipe\sbt-load5964714308503584069_lock, error 5
```
Using sbt shell to reload or build the project avoids this issue:

<img src="./docs/sbtShellUseForReload.jpg" width = "500" height = "230" alt="settings" align="center" />

### Can't analyze dependencies between modules?

Make sure you use one of the following settings to show the way for the plugin:
- The `organization` in [Advanced Setup](#advanced-setup) has been set.
- The `organization` value has been set in `build.sbt` via `ThisBuild` or `inThisBuild`.