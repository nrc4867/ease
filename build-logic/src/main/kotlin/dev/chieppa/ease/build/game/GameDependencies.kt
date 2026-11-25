package dev.chieppa.ease.build.game

import org.gradle.api.Project

/** Where a game's Ease version and Ease checkout come from. */
object GameDependencies {
  /** The Gradle property that pins the exact Ease version a game builds against. */
  const val VERSION_PROPERTY = "ease.version"

  /** The Gradle property naming the Ease checkout a game includes as a composite build. */
  const val DIR_PROPERTY = "ease.dir"

  /** The folder a game looks for Ease in when [DIR_PROPERTY] is not set. */
  const val DEFAULT_DIR = "../ease"

  /** The group every Ease module is published and substituted under. */
  const val GROUP = "dev.chieppa.ease"

  /** The pinned Ease version of [project]; a game without a pin fails its build. */
  fun version(project: Project): String =
    project.providers.gradleProperty(VERSION_PROPERTY).orNull
      ?: error("Pin the Ease version: set $VERSION_PROPERTY in gradle.properties")

  /** The coordinates of the Ease module [name] at the version [project] pins. */
  fun module(project: Project, name: String): String = "$GROUP:$name:${version(project)}"
}
