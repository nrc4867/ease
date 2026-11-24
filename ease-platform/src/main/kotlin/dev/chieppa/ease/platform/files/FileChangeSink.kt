package dev.chieppa.ease.platform.files

import java.nio.file.Path

/** Receives the changes a [FileWatcher] has collected. */
public fun interface FileChangeSink {
  /** [path] (absolute) was [kind]. */
  public fun accept(path: Path, kind: FileChangeKind)
}
