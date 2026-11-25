package dev.chieppa.ease.assets.database

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

private const val IGNORE_FILE = ".gitignore"
private const val IGNORE_ALL = "*\n"

/** The per-machine cache folder of a project, which ignores itself in git. */
internal object CacheFolder {
  /** The cache folder under [root], created with its own `.gitignore` when missing. */
  @Throws(IOException::class)
  fun ensure(root: Path): Path {
    val folder = root.resolve(CACHE_FOLDER)
    Files.createDirectories(folder)
    val ignore = folder.resolve(IGNORE_FILE)
    if (!Files.exists(ignore)) Files.writeString(ignore, IGNORE_ALL)
    return folder
  }
}
