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
  public fun command(): String = script()?.toString()?.replace(BACKSLASH, SLASH) ?: NAME

  private fun script(): Path? {
    val domain = EaseLauncher::class.java.protectionDomain
    val location = domain?.codeSource?.location
    val jar = location?.let { Path.of(it.toURI()) }
    val library = jar?.parent?.takeIf { it.fileName?.toString() == LIBRARY_FOLDER }
    val program = library?.resolveSibling(PROGRAM_FOLDER)
    return program?.resolve(NAME)?.takeIf { Files.isRegularFile(it) }
  }
}
