package dev.chieppa.ease.platform.files

/** What happened to a file. */
public enum class FileChangeKind {
  /** The file appeared. */
  CREATED,

  /** The file's contents changed. */
  MODIFIED,

  /** The file went away. */
  DELETED,
}
