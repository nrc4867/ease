package dev.chieppa.ease.platform.files

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/** Whole-folder operations the JDK has no single call for. */
public object FileTrees {
  /** Deletes [path] and everything under it; does nothing when it does not exist. */
  @Throws(IOException::class)
  public fun deleteRecursively(path: Path) {
    if (!Files.exists(path)) return
    Files.walk(path).use { walk -> walk.sorted(Comparator.reverseOrder()).forEach(Files::delete) }
  }
}
