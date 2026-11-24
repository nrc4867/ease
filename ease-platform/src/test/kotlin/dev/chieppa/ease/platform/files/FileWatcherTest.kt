package dev.chieppa.ease.platform.files

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class FileWatcherTest {
  @TempDir lateinit var folder: Path

  private fun collect(watcher: FileWatcher): List<Pair<Path, FileChangeKind>> {
    val seen = mutableListOf<Pair<Path, FileChangeKind>>()
    watcher.drain { path, kind -> seen += path to kind }
    return seen
  }

  @Test
  fun `a manual watcher hands over what was published, oldest first, then forgets it`() {
    val watcher = ManualFileWatcher()
    val a = folder.resolve("a.txt")
    val b = folder.resolve("b.txt")
    watcher.publish(a, FileChangeKind.CREATED)
    watcher.publish(b, FileChangeKind.DELETED)

    assertEquals(
      listOf(a to FileChangeKind.CREATED, b to FileChangeKind.DELETED),
      collect(watcher),
    )
    assertTrue(collect(watcher).isEmpty())
  }

  @Test
  fun `a manual watcher lists the folders it watches`() {
    val watcher = ManualFileWatcher()

    watcher.watch(folder)

    assertEquals(listOf(folder), watcher.watched)
  }

  @Test
  fun `the JDK watcher reports a file written in a watched folder`() {
    JdkFileWatcher().use { watcher ->
      watcher.watch(folder)
      val file = folder.resolve("note.txt")
      Files.writeString(file, "one")

      val seen = mutableSetOf<Path>()
      while (file !in seen) {
        watcher.awaitChanges { path, _ -> seen.add(path) }
      }

      assertTrue(file in seen)
    }
  }

  @Test
  fun `the JDK watcher reports the files of a folder created under a watched one`() {
    JdkFileWatcher().use { watcher ->
      watcher.watch(folder)
      val inner = folder.resolve("sub").resolve("deep")
      Files.createDirectories(inner)
      val file = inner.resolve("x.bin")
      Files.writeString(file, "x")

      val seen = mutableSetOf<Path>()
      while (file !in seen) {
        watcher.awaitChanges { path, _ -> seen.add(path) }
      }

      assertTrue(file in seen)
    }
  }

  @Test
  fun `draining with nothing collected returns at once`() {
    JdkFileWatcher().use { watcher ->
      watcher.watch(folder)

      assertTrue(collect(watcher).isEmpty())
    }
  }
}
