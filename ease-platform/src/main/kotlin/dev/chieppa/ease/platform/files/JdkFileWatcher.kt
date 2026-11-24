package dev.chieppa.ease.platform.files

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.StandardWatchEventKinds.OVERFLOW
import java.nio.file.WatchKey
import kotlin.io.path.isDirectory

/**
 * A [FileWatcher] on the JDK's `WatchService`. Folders created under a watched folder are watched
 * too, and the files already in them are reported as created, so a folder copied in whole is not
 * missed. Changes to folders themselves are not reported, only to files.
 */
public class JdkFileWatcher : FileWatcher {
  private val service = FileSystems.getDefault().newWatchService()
  private val directories = HashMap<WatchKey, Path>()

  override fun watch(directory: Path) {
    Files.walk(directory).use { walk -> walk.filter { it.isDirectory() }.forEach { register(it) } }
  }

  override fun drain(sink: FileChangeSink) {
    while (true) {
      val key = service.poll() ?: return
      handle(key, sink)
    }
  }

  /**
   * Blocks until a change arrives, then hands it and everything else collected to [sink]. For a
   * thread that exists to watch; the simulation uses [drain].
   */
  public fun awaitChanges(sink: FileChangeSink) {
    handle(service.take(), sink)
    drain(sink)
  }

  override fun close() {
    service.close()
    directories.clear()
  }

  private fun register(directory: Path) {
    val key = directory.register(service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
    directories[key] = directory
  }

  private fun handle(key: WatchKey, sink: FileChangeSink) {
    val directory = directories[key]
    for (event in key.pollEvents()) {
      if (directory == null || event.kind() == OVERFLOW) continue
      val path = directory.resolve(event.context() as Path)
      when (event.kind()) {
        ENTRY_CREATE -> created(path, sink)
        ENTRY_DELETE -> sink.accept(path, FileChangeKind.DELETED)
        else -> if (!path.isDirectory()) sink.accept(path, FileChangeKind.MODIFIED)
      }
    }
    if (!key.reset()) directories.remove(key)
  }

  private fun created(path: Path, sink: FileChangeSink) {
    if (!path.isDirectory()) {
      sink.accept(path, FileChangeKind.CREATED)
      return
    }
    watch(path)
    Files.walk(path).use { walk ->
      walk.filter { !it.isDirectory() }.forEach { sink.accept(it, FileChangeKind.CREATED) }
    }
  }
}
