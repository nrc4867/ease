package dev.chieppa.ease.assets.database

import java.nio.file.Path

private const val HIDDEN_PREFIX = "."

/** Which files of the project folder the asset database looks at. */
internal object ProjectPaths {
  /** Whether [relative] is, or sits in, a folder or file whose name starts with a dot. */
  fun isHidden(relative: Path): Boolean = relative.any { it.toString().startsWith(HIDDEN_PREFIX) }
}
