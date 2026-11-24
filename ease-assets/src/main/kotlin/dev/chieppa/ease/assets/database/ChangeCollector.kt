package dev.chieppa.ease.assets.database

import dev.chieppa.ease.assets.meta.MetaFiles
import dev.chieppa.ease.platform.files.FileChangeKind
import dev.chieppa.ease.platform.files.FileChangeSink
import java.nio.file.Path

/**
 * Gathers the file changes of one drain as the set of source paths they touch: a change to a meta
 * file touches its source, and a source touched twice counts once. A watcher that lost events asks
 * for a rescan of everything instead.
 */
internal class ChangeCollector(private val root: Path) : FileChangeSink {
  private val touched = LinkedHashSet<Path>()
  private var rescan = false

  val isEmpty: Boolean
    get() = touched.isEmpty() && !rescan

  override fun accept(path: Path, kind: FileChangeKind) {
    if (kind == FileChangeKind.OVERFLOWED) {
      rescan = true
    } else if (path.startsWith(root)) {
      val relative = root.relativize(path)
      if (!ProjectPaths.isHidden(relative)) {
        touched.add(if (MetaFiles.isMeta(relative)) MetaFiles.sourceOf(relative) else relative)
      }
    }
  }

  /** Whether events were lost since the last [take], so every path must be looked at. */
  fun needsRescan(): Boolean = rescan

  /** The touched source paths, oldest first, leaving the collector empty. */
  fun take(): List<Path> {
    val paths = touched.toList()
    touched.clear()
    rescan = false
    return paths
  }
}
