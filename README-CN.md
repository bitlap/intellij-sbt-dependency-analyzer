Sbt Dependency Analyzer for IntelliJ IDEA
---------

<img src="./logo.svg" width = "250" height = "150" alt="logo" align="right" />

[![Build](https://github.com/bitlap/intellij-sbt-dependency-analyzer/actions/workflows/ScalaCI.yml/badge.svg)](https://github.com/bitlap/intellij-sbt-dependency-analyzer/actions/workflows/ScalaCI.yml)
[![Version](https://img.shields.io/jetbrains/plugin/v/22427-sbt-dependency-analyzer?label=Version)](https://plugins.jetbrains.com/plugin/22427-sbt-dependency-analyzer/versions)
[![JetBrains Plugin Downloads](https://img.shields.io/jetbrains/plugin/d/22427?label=JetBrains%20Plugin%20Downloads)](https://plugins.jetbrains.com/plugin/22427-sbt-dependency-analyzer)

中文 | [English](README.md)

**如果你喜欢这个项目，或者对你有用，[点击](https://github.com/bitlap/intellij-sbt-dependency-analyzer)右上角 ⭐️ Star 支持下吧~**

## 特性

> 本插件从 IntelliJ IDEA 2023.1（Community、Ultimate 或 Android Studio） 开始支持

- [x] 查看依赖树
- [x] 显示冲突
- [x] 搜索依赖
- [x] 显示模块间依赖关系
- [x] 查看 JAR 包大小
- [x] 依赖定位
  - 点击后将跳转到该依赖在`build.sbt`中的位置
  - 仅对用户定义依赖可用
- [x] 依赖排除（实验性）
  - 选中用户定义依赖下的传递依赖，表示排除；选择中用户定义依赖本身，表示删除该依赖
  - 版本`0.5.0-242.21829.142`及以上支持


## 使用说明

此插件首次分析失败时将自动生成 `project/sdap.sbt` 文件，并在其中插入一行 `addDependencyTreePlugin` （或 `addSbtPlugin(...)` ）。 请勿修改或删除 `project/sdap.sbt` 文件。

此插件依赖于 `sbt-dependency-tree`，这是一个第三方插件，但现在已默认集成到 sbt 中（尽管默认情况下未启用，详见 [sbt 问题](https://github.com/sbt/sbt/pull/5880)）。

**让我们看看如何使用它！**

> 默认快捷键: Ctrl + Shift + L

![image](https://plugins.jetbrains.com/files/22427/screenshot_064531dc-a3fa-4a8e-9437-7e76defa1f48)

## 更多细节

该插件使用以下 sbt 命令。但请放心，插件已经优化，以尽量减少执行的次数：`organization`、`moduleName`、`dependencyDot`、`reload`、`update`

## 高级设置

> 如果不确定，您可以安全地跳过这些配置！

通过使用配置，可以显著减少分析等待时间：

<img src="./docs/settings.png" width="400" height="280" alt="settings" align="right" />

**文件缓存超时**

如果依赖文件（`.dot`）在最近`3600秒`（默认值）内没有修改过，插件将继续使用存在的文件来分析，否则将重新执行`dependencyDot`命令，这是一定程度上的缓存，但在项目首次打开分析图时，缓存可能不生效。

**组织ID**

如果您指定了此值，则将不再使用 `organization` 命令获取项目的组织ID。

**禁用作用域**

如果不需要分析所有作用域，只需禁用您不想要分析的作用域。

配置是持久的，并与每个 IntelliJ 项目相关联。

与其他插件一样，此插件具有自己的存储位置，即 `.idea/bitlap.sbt.dependency.analyzer.xml`。删除此文件将清除缓存。

## 问题排查

### "Caused by: java.io.IOException: Could not create lock for ..."

由于插件需要使用 sbt shell，打开依赖分析视图并随后使用 IntelliJ IDEA 重新加载或构建项目可能会导致以下问题：

```
Caused by: java.io.IOException: Could not create lock for \\.\pipe\sbt-load5964714308503584069_lock, error 5
```

为避免此问题，使用 sbt shell 来重新加载或构建项目：

<img src="./docs/sbtShellUseForReload.jpg" width="500" height="230" alt="settings" align="center" />

### 无法分析模块之间的依赖关系？

请确保您已应用了以下配置之一，以帮助识别正确的模块：

- 在 [高级设置](#高级设置) 中已配置 `organization`。
- 在 `build.sbt` 中使用 `ThisBuild` 或 `inThisBuild` 设置了 `organization` 值。

> 注意：不在根项目的`dependsOn`中的子模块不会被分析，依赖为空。

## 特别感谢

本项目使用 JetBrains IDEA 开发。 感谢 JetBrains 提供的免费许可证。

<a href="www.jetbrains.com">
<img src="https://resources.jetbrains.com/storage/products/company/brand/logos/jb_beam.svg?_gl=1*8f2ovk*_ga*NTY2NTA4Mzg1LjE2NzU3MzgzMTI.*_ga_9J976DJZ68*MTcwMzIwOTE4NS4xODUuMS4xNzAzMjA5NDYzLjI4LjAuMA..&_ga=2.177269094.2105719560.1703209186-566508385.1675738312" alt="IntelliJ IDEA logo.">
</a>

<br />