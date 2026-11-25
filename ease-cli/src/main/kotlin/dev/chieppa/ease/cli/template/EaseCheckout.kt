package dev.chieppa.ease.cli.template

import java.nio.file.Path
import kotlin.io.path.exists

/** Finding out whether a folder is inside a checkout of Ease itself. */
public object EaseCheckout {
  private const val SETTINGS = "settings.gradle.kts"
  private const val CLI_BUILD = "ease-cli/build.gradle.kts"

  /**
   * The root of the Ease checkout [path] is in or under, or null. A checkout is a folder with both
   * the settings file and the command line's module.
   */
  public fun containing(path: Path): Path? =
    generateSequence(path.toAbsolutePath().normalize()) { it.parent }
      .firstOrNull { it.resolve(SETTINGS).exists() && it.resolve(CLI_BUILD).exists() }
}
