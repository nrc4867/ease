package dev.chieppa.ease.assets.database

import dev.chieppa.ease.assets.meta.MetaFiles
import dev.chieppa.ease.platform.files.FileChangeKind
import dev.chieppa.ease.platform.files.FileChangeSink
import java.nio.file.Path

/**
 * Gathers the file changes of one drain as the set of source paths they touch: a change to a meta
 * file touches its source, and a source touched twice counts once.
 */
internal class ChangeCollector(private val root: Path) : FileChangeSink {
  private val touched = LinkedHashSet<Path>()

  val isEmpty: Boolean
    get() = touched.isEmpty()

  override fun accept(path: Path, kind: FileChangeKind) {
    if (!path.startsWith(root)) return
    val relative = root.relativize(path)
    if (ProjectPaths.isHidden(relative)) return
    touched.add(if (MetaFiles.isMeta(relative)) MetaFiles.sourceOf(relative) else relative)
  }

  /** The touched source paths, oldest first, leaving the collector empty. */
  fun take(): List<Path> {
    val paths = touched.toList()
    touched.clear()
    return paths
  }
}
