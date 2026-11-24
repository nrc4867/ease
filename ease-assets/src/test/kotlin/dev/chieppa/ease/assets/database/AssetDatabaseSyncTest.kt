package dev.chieppa.ease.assets.database

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.TestProject
import dev.chieppa.ease.platform.files.FileChangeKind
import dev.chieppa.ease.platform.files.ManualFileWatcher
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class AssetDatabaseSyncTest {
  @TempDir lateinit var folder: Path

  private val watcher = ManualFileWatcher()
  private val project by lazy { TestProject(folder) }
  private val database by lazy { AssetDatabase.load(folder, watcher, project.guids) }

  private fun touched(name: String, kind: FileChangeKind = FileChangeKind.MODIFIED) =
    watcher.publish(database.root.resolve(name), kind)

  @Test
  fun `a changed source is reported with its new hash and its meta file is untouched`() {
    val guid = project.asset("a.txt", "one")
    database
    val metaBefore = Files.readString(project.path("a.txt.meta"))
    Files.writeString(project.path("a.txt"), "two")
    touched("a.txt")

    val change = assertIs<AssetChange.Changed>(database.sync().single())

    assertEquals(guid, change.guid)
    assertEquals(database[guid]?.contentHash, change.record.contentHash)
    assertEquals(metaBefore, Files.readString(project.path("a.txt.meta")))
  }

  @Test
  fun `touching a file without changing it reports nothing`() {
    project.asset("a.txt", "one")
    database
    touched("a.txt")

    assertTrue(database.sync().isEmpty())
  }

  @Test
  fun `many changes to one file in a tick are one change`() {
    project.asset("a.txt", "one")
    database
    Files.writeString(project.path("a.txt"), "two")
    repeat(3) { touched("a.txt") }
    touched("a.txt.meta")

    assertEquals(1, database.sync().size)
  }

  @Test
  fun `a deleted source is reported as removed`() {
    val guid = project.asset("a.txt", "one")
    database
    Files.delete(project.path("a.txt"))
    touched("a.txt", FileChangeKind.DELETED)

    val removed = assertIs<AssetChange.Removed>(database.sync().single())

    assertEquals(guid, removed.guid)
    assertNull(database[guid])
    assertTrue(database.problems.single() is AssetProblem.MetaWithoutSource)
  }

  @Test
  fun `a deleted folder removes the assets under it`() {
    project.asset("art/a.png", "a")
    project.asset("art/deep/b.png", "b")
    database
    project.path("art").toFile().deleteRecursively()
    touched("art", FileChangeKind.DELETED)

    val changes = database.sync()

    assertEquals(2, changes.filterIsInstance<AssetChange.Removed>().size)
    assertTrue(database.assets.isEmpty())
  }

  @Test
  fun `a deleted folder clears the problems under it, whatever they were about`() {
    project.asset("dir/old.png", "pixels")
    database
    Files.delete(project.path("dir/old.png"))
    project.write("dir/new.png", "different")
    touched("dir/old.png", FileChangeKind.DELETED)
    touched("dir/new.png", FileChangeKind.CREATED)
    database.sync()
    assertEquals(2, database.problems.size)
    project.path("dir").toFile().deleteRecursively()
    touched("dir", FileChangeKind.DELETED)

    database.sync()

    assertTrue(database.problems.isEmpty())
  }

  @Test
  fun `a file moved with its meta file is one move that keeps the GUID`() {
    val guid = project.asset("old/a.png", "pixels")
    database
    Files.createDirectories(project.path("new"))
    Files.move(project.path("old/a.png"), project.path("new/a.png"))
    Files.move(project.path("old/a.png.meta"), project.path("new/a.png.meta"))
    touched("old/a.png", FileChangeKind.DELETED)
    touched("old/a.png.meta", FileChangeKind.DELETED)
    touched("new/a.png", FileChangeKind.CREATED)
    touched("new/a.png.meta", FileChangeKind.CREATED)

    val moved = assertIs<AssetChange.Moved>(database.sync().single())

    assertEquals(guid, moved.guid)
    assertEquals(Path.of("old", "a.png"), moved.from)
    assertEquals(Path.of("new", "a.png"), database[guid]?.path)
  }

  @Test
  fun `a new source is a problem until it is adopted`() {
    database
    project.write("fresh.txt", "new")
    touched("fresh.txt", FileChangeKind.CREATED)

    assertTrue(database.sync().isEmpty())
    assertTrue(database.problems.single() is AssetProblem.SourceWithoutMeta)

    val record = database.adopt(Path.of("fresh.txt"))

    assertEquals(record, database.at(Path.of("fresh.txt")))
    assertTrue(database.problems.isEmpty())
  }

  @Test
  fun `a source whose meta file was copied by hand is a duplicate GUID problem`() {
    val guid = project.asset("a.txt", "one")
    database
    copyAssetTo("b.txt")

    database.sync()

    val problem = assertIs<AssetProblem.DuplicateGuid>(database.problems.single())
    assertEquals(guid, problem.guid)
    assertEquals(Path.of("a.txt"), database[guid]?.path)
  }

  @Test
  fun `a duplicate GUID is settled when the other holder goes away`() {
    val guid = project.asset("a.txt", "one")
    database
    copyAssetTo("b.txt")
    database.sync()
    Files.delete(project.path("a.txt"))
    Files.delete(project.path("a.txt.meta"))
    touched("a.txt", FileChangeKind.DELETED)
    touched("a.txt.meta", FileChangeKind.DELETED)

    val moved = assertIs<AssetChange.Moved>(database.sync().single())

    assertEquals(guid, moved.guid)
    assertEquals(Path.of("b.txt"), database[guid]?.path)
    assertTrue(database.problems.isEmpty())
  }

  @Test
  fun `a duplicate GUID is settled when the other holder takes another GUID`() {
    val guid = project.asset("a.txt", "one")
    database
    copyAssetTo("b.txt")
    database.sync()
    project.asset("a.txt", "one", Guid(5, 5))
    touched("a.txt.meta", FileChangeKind.MODIFIED)

    database.sync()

    assertEquals(Path.of("b.txt"), database[guid]?.path)
    assertEquals(Path.of("a.txt"), database[Guid(5, 5)]?.path)
    assertTrue(database.problems.isEmpty())
  }

  @Test
  fun `a meta file left behind finds its moved source through the cached hash`() {
    val guid = project.asset("a.png", "pixels")
    database
    Files.move(project.path("a.png"), project.path("b.png"))
    touched("a.png", FileChangeKind.DELETED)
    touched("b.png", FileChangeKind.CREATED)

    val moved = assertIs<AssetChange.Moved>(database.sync().single())

    assertEquals(guid, moved.guid)
    assertEquals(Path.of("b.png"), database[guid]?.path)
    assertTrue(database.problems.isEmpty())
  }

  @Test
  fun `lost events make the database look at every file again`() {
    val guid = project.asset("a.txt", "one")
    database
    Files.writeString(project.path("a.txt"), "two")
    project.asset("b.txt", "bee")
    watcher.publish(database.root, FileChangeKind.OVERFLOWED)

    val changes = database.sync()

    assertEquals(guid, changes.filterIsInstance<AssetChange.Changed>().single().guid)
    assertEquals(2, database.assets.size)
  }

  @Test
  fun `adopting a file that has a meta file is refused`() {
    project.asset("a.txt", "one")
    database

    assertFailsWith<IllegalStateException> { database.adopt(Path.of("a.txt")) }
  }

  @Test
  fun `changes outside the project and in hidden folders are ignored`() {
    project.asset("a.txt", "one")
    database
    watcher.publish(folder.resolveSibling("elsewhere.txt"), FileChangeKind.MODIFIED)
    touched(".git/index")

    assertTrue(database.sync().isEmpty())
  }

  private fun copyAssetTo(name: String) {
    project.write(name, "copy")
    Files.copy(project.path("a.txt.meta"), project.path("$name.meta"))
    touched(name, FileChangeKind.CREATED)
    touched("$name.meta", FileChangeKind.CREATED)
  }
}
