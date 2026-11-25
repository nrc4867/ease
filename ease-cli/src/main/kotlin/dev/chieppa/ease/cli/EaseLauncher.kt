package dev.chieppa.ease.cli

import java.nio.file.Files
import java.nio.file.Path

/** Finds the `ease` program that is running, so git hooks and merge drivers can run it again. */
public object EaseLauncher {
  private const val NAME = "ease"
  private const val BACKSLASH = '\\'
  private const val SLASH = '/'
  private const val LIBRARY_FOLDER = "lib"
  private const val PROGRAM_FOLDER = "bin"

  /**
   * The command that starts this installation of `ease`: the start script beside the `lib` folder
   * the running code was loaded from, written with `/`; plain `ease` when it runs from anywhere
   * else, such as Gradle.
   */
  public fun command(): String {
    val location = EaseLauncher::class.java.protectionDomain?.codeSource?.location ?: return NAME
    val jar = Path.of(location.toURI())
    val library = jar.parent
    if (library?.fileName?.toString() != LIBRARY_FOLDER) return NAME
    val script = library.resolveSibling(PROGRAM_FOLDER).resolve(NAME)
    return if (Files.isRegularFile(script)) script.toString().replace(BACKSLASH, SLASH) else NAME
  }
}
