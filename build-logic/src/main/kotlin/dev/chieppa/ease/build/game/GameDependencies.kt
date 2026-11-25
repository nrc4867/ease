package dev.chieppa.ease.build.game

import java.io.File
import java.util.Properties
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

  /** The class the player runs a game from. */
  const val PLAYER_MAIN = "dev.chieppa.ease.player.PlayerKt"

  /** The properties file of an Ease checkout, where its own version is. */
  const val ENGINE_PROPERTIES = "gradle.properties"

  /** The pinned Ease version of [project]; a game without a pin fails its build. */
  fun version(project: Project): String =
    project.providers.gradleProperty(VERSION_PROPERTY).orNull
      ?: error("Pin the Ease version: set $VERSION_PROPERTY in gradle.properties")

  /** The coordinates of the Ease module [name] at the version [project] pins. */
  fun module(project: Project, name: String): String = "$GROUP:$name:${version(project)}"

  /** The version the Ease checkout in [easeDir] builds, or null when it declares none. */
  fun checkoutVersion(easeDir: File): String? {
    val file = easeDir.resolve(ENGINE_PROPERTIES)
    if (!file.isFile) return null
    return Properties().apply { file.reader().use { load(it) } }.getProperty(VERSION_PROPERTY)
  }

  /**
   * Fails, naming both versions and the fix, when [pinned] is not the version of the checkout in
   * [easeDir]: a composite build takes whatever the checkout holds, whatever the game pins.
   */
  fun requirePinMatches(easeDir: File, pinned: String?) {
    val pin = pinned ?: error("Pin the Ease version: set $VERSION_PROPERTY in gradle.properties")
    val actual = checkoutVersion(easeDir)
    check(actual == pin) {
      "This game pins Ease $pin ($VERSION_PROPERTY in gradle.properties), but the checkout in " +
        "$easeDir is ${actual ?: "of no known version"}. Check out Ease $pin, point $DIR_PROPERTY " +
        "at it, or change the pin to move the game to ${actual ?: "another"} version."
    }
  }
}
