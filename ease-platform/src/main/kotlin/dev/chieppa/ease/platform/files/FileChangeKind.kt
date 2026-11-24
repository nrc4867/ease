package dev.chieppa.ease.platform.files

/** What happened to a file. */
public enum class FileChangeKind {
  /** The file appeared. */
  CREATED,

  /** The file's contents changed. */
  MODIFIED,

  /** The file went away. */
  DELETED,

  /**
   * Changes under the path were lost, as when a burst of thousands overflows the system's queue.
   * The listener treats everything under the path as possibly changed and rescans it.
   */
  OVERFLOWED,
}
