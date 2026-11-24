package dev.chieppa.ease.platform.files

import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.StandardWatchEventKinds.OVERFLOW
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import kotlin.io.path.isDirectory

/**
 * A [FileWatcher] on the JDK's `WatchService`. Folders created under a watched folder are watched
 * too, and the files already in them are reported as created, so a folder copied in whole is not
 * missed. Changes to folders themselves are not reported, only to files. When the system drops
 * events, the folder concerned is reported as [FileChangeKind.OVERFLOWED].
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
    try {
      if (directory != null) key.pollEvents().forEach { report(directory, it, sink) }
    } finally {
      if (!key.reset()) directories.remove(key)
    }
  }

  /**
   * Passes one event on. A folder that vanishes while it is being read raises an I/O error, which
   * is dropped: its deletion arrives as an event of its own.
   */
  @Suppress("SwallowedException")
  private fun report(directory: Path, event: WatchEvent<*>, sink: FileChangeSink) {
    try {
      if (event.kind() == OVERFLOW) {
        sink.accept(directory, FileChangeKind.OVERFLOWED)
      } else {
        change(directory.resolve(event.context() as Path), event.kind(), sink)
      }
    } catch (_: IOException) {
      return
    } catch (_: UncheckedIOException) {
      return
    }
  }

  private fun change(path: Path, kind: WatchEvent.Kind<*>, sink: FileChangeSink) {
    when (kind) {
      ENTRY_CREATE -> created(path, sink)
      ENTRY_DELETE -> sink.accept(path, FileChangeKind.DELETED)
      else -> if (!path.isDirectory()) sink.accept(path, FileChangeKind.MODIFIED)
    }
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
