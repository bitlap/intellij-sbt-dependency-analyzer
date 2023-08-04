Sbt Dependency Analyzer for IntelliJ IDEA
---------

<img src="./logo.png" width = "200" height = "100" alt="logo" align="right" />

[![Build](https://github.com/bitlap/intellij-sbt-dependency-analyzer/actions/workflows/ScalaCI.yml/badge.svg)](https://github.com/bitlap/intellij-sbt-dependency-analyzer/actions/workflows/ScalaCI.yml)

## Environment

- Java 11
- Scala 3.3.0
- Intellij 231.9392.1

## Features

- Same features as the official Gradle Dependency Analyzer

## Preview

### Conflict

![](./docs/dependencyTree1.png)

### Note 

If both the Gradle and SBT plugins are enabled in the environment, two analysis buttons will appear. Please try the latter one. (Generally speaking)

![](./docs/dependencyTree2.jpg)

> The plugin currently has no depth limit, pay attention to memory.
