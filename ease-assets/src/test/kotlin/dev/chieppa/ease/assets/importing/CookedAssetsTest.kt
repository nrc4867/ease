package dev.chieppa.ease.assets.importing

import dev.chieppa.ease.assets.ContentHash
import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.TestProject
import dev.chieppa.ease.assets.associations.FileAssociationRegistry
import dev.chieppa.ease.assets.associations.FileMatch
import dev.chieppa.ease.assets.database.AssetDatabase
import dev.chieppa.ease.assets.loading.AssetLoadException
import dev.chieppa.ease.assets.loading.AssetLoader
import dev.chieppa.ease.assets.loading.AssetStore
import dev.chieppa.ease.platform.files.FileChangeKind
import dev.chieppa.ease.platform.files.ManualFileWatcher
import java.nio.file.Path
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.io.TempDir

/** Importers that read files beside their source, and cooked assets loaded by GUID. */
class CookedAssetsTest {
  @TempDir lateinit var folder: Path

  private val project by lazy { TestProject(folder) }
  private val associations = FileAssociationRegistry()
  private val watcher = ManualFileWatcher()

  /**
   * Cooks `joined.text`, the source followed by the file its first line names beside it, and
   * `count.note`, the number of characters.
   */
  private class Joiner : Importer {
    override val id = "joiner"
    override val version = 1
    var runs = 0

    override fun import(source: ByteArray, settings: JsonObject): List<CookedAsset> =
      error("needs its context")

    override fun import(
      source: ByteArray,
      settings: JsonObject,
      context: ImportContext,
    ): List<CookedAsset> {
      runs++
      val text = source.toString(Charsets.UTF_8)
      val other = context.read(text.lines().first()).toString(Charsets.UTF_8)
      return listOf(
        CookedAsset("joined.text", "$text+$other".toByteArray()),
        CookedAsset("count.note", text.length.toString().toByteArray()),
      )
    }
  }

  private object TextLoader : AssetLoader<String> {
    override val type: KClass<String> = String::class

    override fun load(guid: Guid, bytes: ByteArray): String = bytes.toString(Charsets.UTF_8)
  }

  private val joiner = Joiner()

  init {
    associations.register(Importer.kind, FileMatch.Extension("join"), 0, joiner)
    associations.register(AssetLoader.kind, FileMatch.Extension("text"), 0, TextLoader)
  }

  private fun open(): Pair<AssetDatabase, ImportService> {
    val database = AssetDatabase.open(folder, watcher, project.guids, associations)
    return database to ImportService(database, associations).also { it.refresh() }
  }

  @Test
  fun `a file read beside the source is part of its key`() {
    project.asset("parts/tail.txt", "one")
    project.asset("parts/a.join", "tail.txt")
    val (_, imports) = open()

    imports.refresh()
    project.write("parts/tail.txt", "two")
    imports.refresh()

    assertEquals(2, joiner.runs)
  }

  @Test
  fun `a cooked asset loads by its sub-asset GUID and the source by the one that fits`() {
    project.asset("tail.txt", "end")
    val source = project.asset("a.join", "tail.txt")
    val (database, imports) = open()
    val store = AssetStore(database, associations, imports)

    val bySub = store.handle(source.named("joined.text"), String::class)
    val bySource = store.handle(source, String::class)

    assertEquals("tail.txt+end", bySub.value)
    assertEquals("tail.txt+end", bySource.value)
    assertTrue(store.has(source.named("count.note")))
    assertFailsWith<AssetLoadException> { store.handle(source.named("count.note"), String::class) }
  }

  @Test
  fun `changing a file the import read reimports and reloads the cooked handles`() {
    val tail = project.asset("tail.txt", "end")
    val source = project.asset("a.join", "tail.txt")
    val (database, imports) = open()
    val store = AssetStore(database, associations, imports)
    val handle = store.handle(source.named("joined.text"), String::class)

    project.write("tail.txt", "new end")
    database.sync()
    store.reload(tail, ContentHash.of(project.path("tail.txt")))

    assertEquals("tail.txt+new end", handle.value)
    assertEquals(2, handle.version)
  }

  @Test
  fun `a missing file that appears later imports its source with no handle open`() {
    val source = project.asset("a.join", "tail.txt")
    val (database, imports) = open()
    val store = AssetStore(database, associations, imports)
    assertEquals(1, imports.problems.size)

    val tail = project.asset("tail.txt", "late")
    watcher.publish(project.path("tail.txt"), FileChangeKind.CREATED)
    database.sync()
    assertTrue(store.follows(tail))
    store.reload(tail, ContentHash.of(project.path("tail.txt")))

    assertEquals(emptyList(), imports.problems)
    assertEquals("tail.txt+late", store.handle(source.named("joined.text"), String::class).value)
  }

  @Test
  fun `a replaced value that holds resources closes at the second frame boundary`() {
    associations.register(AssetLoader.kind, FileMatch.Extension("note"), 0, NoteLoader)
    project.asset("tail.txt", "end")
    val source = project.asset("a.join", "tail.txt")
    val (database, imports) = open()
    val store = AssetStore(database, associations, imports)
    val first = store.handle(source.named("count.note"), Note::class).value

    project.write("a.join", "tail.txt\nlonger")
    database.sync()
    store.reload(source, ContentHash.of(project.path("a.join")))
    store.frameBoundary()
    val openAfterOne = !first.closed
    store.frameBoundary()

    assertTrue(openAfterOne, "closed while a frame may still draw it")
    assertTrue(first.closed)
    assertEquals(0, store.retiredCount)
  }

  /** A loaded value that holds a resource. */
  private class Note(val text: String) : AutoCloseable {
    var closed = false

    override fun close() {
      closed = true
    }
  }

  private object NoteLoader : AssetLoader<Note> {
    override val type: KClass<Note> = Note::class

    override fun load(guid: Guid, bytes: ByteArray): Note = Note(bytes.toString(Charsets.UTF_8))
  }

  @Test
  fun `a file outside the content folder is refused`() {
    project.asset("a.join", "../outside.txt")
    val (_, imports) = open()

    val problem = imports.problems.single()
    assertTrue(problem.message.contains("outside the content folder"))
  }
}
