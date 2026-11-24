package dev.chieppa.ease.runtime

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.associations.FileAssociationRegistry
import dev.chieppa.ease.assets.associations.FileMatch
import dev.chieppa.ease.assets.database.AssetDatabase
import dev.chieppa.ease.assets.loading.AssetHandle
import dev.chieppa.ease.assets.loading.AssetLoader
import dev.chieppa.ease.assets.loading.AssetStore
import dev.chieppa.ease.assets.meta.AssetMeta
import dev.chieppa.ease.assets.meta.MetaFiles
import dev.chieppa.ease.platform.files.FileChangeKind
import dev.chieppa.ease.platform.files.ManualFileWatcher
import dev.chieppa.ease.runtime.loop.FixedStep
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.io.TempDir

/** Files that change in awkward ways while a world runs: no tick may fail because of one. */
class HostedWorldAssetChurnTest {
  @TempDir lateinit var folder: Path

  private val watcher = ManualFileWatcher()
  private val a = Guid(0, 1)
  private val b = Guid(0, 2)
  private val c = Guid(0, 3)

  /** Loads text; runs [whileLoading] on each load after the first one of "a.txt". */
  private class TextLoader : AssetLoader<String> {
    override val type: KClass<String> = String::class
    var whileLoading: () -> Unit = {}

    override fun load(guid: Guid, bytes: ByteArray): String {
      val text = bytes.decodeToString()
      if (text.startsWith("a2")) whileLoading()
      return text
    }
  }

  private val loader = TextLoader()

  private fun put(name: String, text: String, guid: Guid) {
    val file = folder.resolve(name)
    Files.writeString(file, text)
    MetaFiles.write(MetaFiles.metaOf(file), AssetMeta(guid = guid))
  }

  private fun host(
    vararg assets: Pair<String, Guid>
  ): Pair<HostedWorld, List<AssetHandle<String>>> {
    assets.forEach { (name, guid) -> put(name, name.substringBefore('.') + "1", guid) }
    val database = AssetDatabase.load(folder, watcher)
    val associations = FileAssociationRegistry()
    associations.register(AssetLoader.kind, FileMatch.Extension("txt"), 0, loader)
    val store = AssetStore(database, associations)
    val handles = assets.map { store.handle(it.second, String::class) }
    return HostedWorld(DriftModule(), FixedStep.perSecond(), assets = store) to handles
  }

  private fun changed(name: String, kind: FileChangeKind = FileChangeKind.MODIFIED) =
    watcher.publish(folder.toAbsolutePath().normalize().resolve(name), kind)

  @Test
  fun `a file that changes again between the sync and its reload does not fail the tick`() {
    val (hosted, handles) = host("a.txt" to a, "b.txt" to b, "c.txt" to c)
    put("a.txt", "a2", a)
    put("b.txt", "b2", b)
    put("c.txt", "c2", c)
    listOf("a.txt", "b.txt", "c.txt").forEach { changed(it) }
    loader.whileLoading = { Files.writeString(folder.resolve("b.txt"), "b-mid-save") }

    hosted.step()

    assertEquals("a2", handles[0].value)
    assertEquals("b1", handles[1].value)
    assertNotNull(handles[1].error)
    assertEquals("c2", handles[2].value)
  }

  @Test
  fun `the file that changed mid-save loads once its own change arrives`() {
    val (hosted, handles) = host("a.txt" to a, "b.txt" to b)
    put("a.txt", "a2", a)
    put("b.txt", "b2", b)
    changed("a.txt")
    changed("b.txt")
    loader.whileLoading = { Files.writeString(folder.resolve("b.txt"), "b-mid-save") }
    hosted.step()
    loader.whileLoading = {}

    changed("b.txt")
    hosted.step()

    assertEquals("b-mid-save", handles[1].value)
    assertNull(handles[1].error)
  }

  @Test
  fun `an asset deleted and then recreated loads again`() {
    val (hosted, handles) = host("a.txt" to a)
    Files.delete(folder.resolve("a.txt"))
    Files.delete(folder.resolve("a.txt.meta"))
    changed("a.txt", FileChangeKind.DELETED)
    changed("a.txt.meta", FileChangeKind.DELETED)
    hosted.step()
    assertNotNull(handles[0].error)

    put("a.txt", "reborn", a)
    changed("a.txt", FileChangeKind.CREATED)
    changed("a.txt.meta", FileChangeKind.CREATED)
    hosted.step()

    assertEquals("reborn", handles[0].value)
    assertNull(handles[0].error)
  }

  @Test
  fun `an asset moved and edited in one sync loads its new contents`() {
    val (hosted, handles) = host("a.txt" to a)
    Files.delete(folder.resolve("a.txt"))
    Files.delete(folder.resolve("a.txt.meta"))
    put("moved.txt", "edited while moving", a)
    changed("a.txt", FileChangeKind.DELETED)
    changed("a.txt.meta", FileChangeKind.DELETED)
    changed("moved.txt", FileChangeKind.CREATED)
    changed("moved.txt.meta", FileChangeKind.CREATED)

    hosted.step()

    assertEquals("edited while moving", handles[0].value)
  }
}
