package dev.chieppa.ease.platform.files

import java.nio.file.Path

/**
 * Watches folders for file changes. Changes pile up inside the watcher; the owner collects them
 * with [drain] at a point of its choosing, such as a tick boundary, so no change ever arrives in
 * the middle of a tick. Code that reacts to files takes a watcher from its constructor, so tests
 * pass a [ManualFileWatcher].
 */
public interface FileWatcher : AutoCloseable {
  /** Starts watching [directory] and every folder under it, now or later created. */
  public fun watch(directory: Path)

  /**
   * Hands every change collected since the last drain to [sink], oldest first, and forgets them.
   * Never waits for a change: with none collected it returns at once.
   */
  public fun drain(sink: FileChangeSink)
}
