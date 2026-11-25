package dev.chieppa.ease.assets.database

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.TestProject
import dev.chieppa.ease.assets.associations.FileAssociationRegistry
import dev.chieppa.ease.assets.associations.FileMatch
import dev.chieppa.ease.assets.associations.FolderAsset
import dev.chieppa.ease.assets.folders.FolderPack
import dev.chieppa.ease.assets.loading.AssetLoader
import dev.chieppa.ease.assets.loading.AssetStore
import dev.chieppa.ease.assets.meta.AssetMeta
import dev.chieppa.ease.assets.meta.MetaFiles
import dev.chieppa.ease.platform.files.FileChangeKind
import dev.chieppa.ease.platform.files.ManualFileWatcher
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.io.TempDir

/** A folder a folder-asset contributor names is one asset, its files that asset's contents. */
class FolderAssetTest {
  @TempDir lateinit var folder: Path

  private val watcher = ManualFileWatcher()
  private val project by lazy { TestProject(folder) }
  private val associations = FileAssociationRegistry()
  private val guid = Guid(0, 42)

  private object Level : FolderAsset

  /** Reads a level folder as the names of its files. */
  private object FileNames : AssetLoader<String> {
    override val type: KClass<String> = String::class

    override fun load(guid: Guid, bytes: ByteArray): String =
      FolderPack.unpack(bytes).keys.joinToString(",")
  }

  private fun level() {
    project.write("hall.level/head.yaml", "a: 1\n")
    project.write("hall.level/rooms/one.yaml", "b: 2\n")
    MetaFiles.write(MetaFiles.metaOf(project.path("hall.level")), AssetMeta(guid = guid))
  }

  private fun load(): AssetDatabase {
    associations.register(FolderAsset.kind, FileMatch.Extension("level"), 0, Level)
    return AssetDatabase.load(folder, watcher, project.guids, associations)
  }

  @Test
  fun `a named folder is one asset whose files need no meta files`() {
    level()

    val database = load()

    assertEquals(Path.of("hall.level"), database[guid]?.path)
    assertEquals(1, database.assets.size)
    assertTrue(database.problems.isEmpty())
  }

  @Test
  fun `without the contributor the folder's files are sources without meta files`() {
    level()

    val rejected =
      assertFailsWith<AssetDatabaseRejected> {
        AssetDatabase.load(folder, watcher, project.guids, associations)
      }
    assertEquals(3, rejected.problems.size)
  }

  @Test
  fun `a change to any file inside changes the folder's asset`() {
    level()
    val database = load()
    val before = database[guid]?.contentHash
    Files.writeString(project.path("hall.level/rooms/one.yaml"), "b: 3\n")
    watcher.publish(project.path("hall.level/rooms/one.yaml"), FileChangeKind.MODIFIED)

    val change = assertIs<AssetChange.Changed>(database.sync().single())

    assertEquals(guid, change.guid)
    assertTrue(change.record.contentHash != before)
  }

  @Test
  fun `adding and deleting files inside changes the asset, deleting the folder removes it`() {
    level()
    val database = load()
    project.write("hall.level/rooms/two.yaml", "c: 1\n")
    watcher.publish(project.path("hall.level/rooms/two.yaml"), FileChangeKind.CREATED)
    assertIs<AssetChange.Changed>(database.sync().single())

    project.path("hall.level").toFile().deleteRecursively()
    watcher.publish(project.path("hall.level"), FileChangeKind.DELETED)

    assertIs<AssetChange.Removed>(database.sync().single())
    assertNull(database[guid])
  }

  @Test
  fun `the store loads a folder asset from its pack`() {
    level()
    val database = load()
    associations.register(AssetLoader.kind, FileMatch.Extension("level"), 0, FileNames)

    val names = AssetStore(database, associations).handle(guid, String::class).value

    assertEquals("head.yaml,rooms/one.yaml", names)
  }

  @Test
  fun `a folder that cannot be read whole fails as an IOException`() {
    level()
    val locked = project.path("hall.level/rooms").toFile()
    assumeTrue(locked.setReadable(false) && !Files.isReadable(locked.toPath()))
    try {
      assertFailsWith<IOException> { FolderPack.pack(project.path("hall.level")) }
    } finally {
      locked.setReadable(true)
    }
  }

  @Test
  fun `a pack reads back as the files it was made from, and refuses other bytes`() {
    val files = mapOf("b.yaml" to byteArrayOf(1, 2), "a/c.yaml" to byteArrayOf())

    val unpacked = FolderPack.unpack(FolderPack.pack(files))

    assertEquals(listOf("a/c.yaml", "b.yaml"), unpacked.keys.toList())
    assertContentEquals(byteArrayOf(1, 2), unpacked["b.yaml"])
    assertFailsWith<IllegalArgumentException> { FolderPack.unpack(byteArrayOf(1, 2, 3)) }
    assertFailsWith<IllegalArgumentException> {
      FolderPack.unpack(FolderPack.pack(files).copyOf(20))
    }
  }
}
