package dev.chieppa.ease.platform.files

import java.nio.file.Path

/** A watcher that sees only the changes it is told about, for tests and headless runs. */
public class ManualFileWatcher : FileWatcher {
  private val pending = ArrayDeque<Change>()
  private val directories = mutableListOf<Path>()

  /** The folders being watched, in the order they were added. */
  public val watched: List<Path>
    get() = directories

  override fun watch(directory: Path) {
    directories.add(directory)
  }

  /** Records that [path] was [kind], for the next [drain]. */
  public fun publish(path: Path, kind: FileChangeKind) {
    pending.addLast(Change(path, kind))
  }

  override fun drain(sink: FileChangeSink) {
    while (true) {
      val change = pending.removeFirstOrNull() ?: return
      sink.accept(change.path, change.kind)
    }
  }

  override fun close() {
    pending.clear()
    directories.clear()
  }

  private class Change(val path: Path, val kind: FileChangeKind)
}
