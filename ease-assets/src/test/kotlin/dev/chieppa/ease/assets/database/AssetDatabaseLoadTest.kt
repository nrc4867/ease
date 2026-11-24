package dev.chieppa.ease.assets.database

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.TestProject
import dev.chieppa.ease.assets.meta.MetaFiles
import dev.chieppa.ease.platform.files.ManualFileWatcher
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class AssetDatabaseLoadTest {
  @TempDir lateinit var folder: Path

  private val watcher = ManualFileWatcher()
  private val project by lazy { TestProject(folder) }

  private fun load() = AssetDatabase.load(folder, watcher, project.guids)

  @Test
  fun `loading indexes every source by GUID and by path, and watches the folder`() {
    val rock = project.asset("art/rock.png", "rock")
    val song = project.asset("audio/song.ogg", "song")

    val database = load()

    assertEquals(setOf(rock, song), database.assets.map { it.guid }.toSet())
    assertEquals(Path.of("art", "rock.png"), database[rock]?.path)
    assertEquals(song, database.at(Path.of("audio", "song.ogg"))?.guid)
    assertEquals(listOf(database.root), watcher.watched)
    assertTrue(database.problems.isEmpty())
  }

  @Test
  fun `a GUID used twice rejects the load, naming both files`() {
    val shared = project.asset("a.txt", "one")
    project.asset("b.txt", "two", shared)

    val rejected = assertFailsWith<AssetDatabaseRejected> { load() }

    val message = rejected.message.orEmpty()
    assertTrue("a.txt" in message && "b.txt" in message && shared.toString() in message)
  }

  @Test
  fun `a meta file without a source rejects the load, naming it`() {
    project.asset("gone.txt", "bye")
    Files.delete(project.path("gone.txt"))

    val rejected = assertFailsWith<AssetDatabaseRejected> { load() }

    assertTrue(rejected.problems.single() is AssetProblem.MetaWithoutSource)
    assertTrue("gone.txt.meta" in rejected.message.orEmpty())
  }

  @Test
  fun `a source without a meta file rejects the load, naming it`() {
    project.write("loose.txt", "no meta")

    val rejected = assertFailsWith<AssetDatabaseRejected> { load() }

    assertTrue(rejected.problems.single() is AssetProblem.SourceWithoutMeta)
    assertTrue("loose.txt" in rejected.message.orEmpty())
  }

  @Test
  fun `a meta file that cannot be read rejects the load, naming it`() {
    project.write("odd.txt", "x")
    project.write("odd.txt.meta", "formatVersion: 1\nguid: nope\ncontentHash: x\n")

    val rejected = assertFailsWith<AssetDatabaseRejected> { load() }

    assertTrue(rejected.problems.single() is AssetProblem.UnreadableMeta)
  }

  @Test
  fun `a moved source keeps its GUID because its meta file moved with it`() {
    val guid = project.asset("old/hero.png", "pixels")
    Files.createDirectories(project.path("new"))
    Files.move(project.path("old/hero.png"), project.path("new/hero.png"))
    Files.move(project.path("old/hero.png.meta"), project.path("new/hero.png.meta"))

    val database = load()

    assertEquals(Path.of("new", "hero.png"), database[guid]?.path)
  }

  @Test
  fun `a meta file left behind finds its moved source through the hash the last run cached`() {
    val guid = project.asset("old/hero.png", "pixels")
    load()
    Files.createDirectories(project.path("new"))
    Files.move(project.path("old/hero.png"), project.path("new/renamed.png"))

    val database = load()

    assertEquals(Path.of("new", "renamed.png"), database[guid]?.path)
    assertTrue(Files.exists(MetaFiles.metaOf(project.path("new/renamed.png"))))
    assertTrue(Files.notExists(project.path("old/hero.png.meta")))
    assertTrue(database.problems.isEmpty())
  }

  @Test
  fun `without a cache, as in a fresh clone, a left behind meta file is a problem`() {
    project.asset("old/hero.png", "pixels")
    load()
    project.path(".ease").toFile().deleteRecursively()
    Files.move(project.path("old/hero.png"), project.path("renamed.png"))

    val rejected = assertFailsWith<AssetDatabaseRejected> { load() }

    assertEquals(
      setOf(AssetProblem.MetaWithoutSource::class, AssetProblem.SourceWithoutMeta::class),
      rejected.problems.map { it::class }.toSet(),
    )
  }

  @Test
  fun `a hash two new files share does not pick one of them`() {
    project.asset("old/hero.png", "pixels")
    load()
    Files.delete(project.path("old/hero.png"))
    project.write("a.png", "pixels")
    project.write("b.png", "pixels")

    val rejected = assertFailsWith<AssetDatabaseRejected> { load() }

    assertEquals(3, rejected.problems.size)
  }

  @Test
  fun `hashes are cached under a hidden folder that git ignores, never in meta files`() {
    project.asset("art/rock.png", "rock")

    val database = load()

    val cache = project.path(".ease/cache")
    assertTrue(Files.isRegularFile(cache.resolve("asset-hashes.yaml")))
    assertEquals("*\n", Files.readString(cache.resolve(".gitignore")))
    assertEquals(1, database.assets.size)
    assertTrue(!Files.readString(project.path("art/rock.png.meta")).contains("sha256"))
  }

  @Test
  fun `hidden folders are not assets`() {
    project.asset("art/rock.png", "rock")
    project.write(".git/config", "[core]")
    project.write(".gitignore", "cache/")

    val database = load()

    assertEquals(1, database.assets.size)
  }

  @Test
  fun `loading does not rewrite meta files`() {
    val guid = project.asset("a.txt", "one")
    Files.writeString(project.path("a.txt"), "edited since")
    val before = Files.readString(project.path("a.txt.meta"))

    val database = load()

    assertNotNull(database[guid])
    assertEquals(before, Files.readString(project.path("a.txt.meta")))
    assertNull(database[Guid(9, 9)])
  }
}
