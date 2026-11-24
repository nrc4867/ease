package dev.chieppa.ease.assets.loading

import dev.chieppa.ease.assets.ContentHash
import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.TestProject
import dev.chieppa.ease.assets.associations.FileAssociationRegistry
import dev.chieppa.ease.assets.associations.FileMatch
import dev.chieppa.ease.assets.database.AssetDatabase
import dev.chieppa.ease.platform.files.ManualFileWatcher
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class AssetStoreTest {
  @TempDir lateinit var folder: Path

  private val project by lazy { TestProject(folder) }
  private val associations = FileAssociationRegistry()
  private val database by lazy { AssetDatabase.load(folder, ManualFileWatcher(), project.guids) }
  private val store by lazy { AssetStore(database, associations) }

  private object TextLoader : AssetLoader<String> {
    override val type: KClass<String> = String::class

    override fun load(guid: Guid, bytes: ByteArray): String {
      val text = bytes.decodeToString()
      require(!text.startsWith("!")) { "broken text" }
      return text
    }
  }

  private fun withTextLoader() {
    associations.register(AssetLoader.kind, FileMatch.Extension("txt"), 0, TextLoader)
  }

  @Test
  fun `a handle loads the file through the loader its type picks`() {
    val guid = project.asset("a.txt", "hello")
    withTextLoader()

    val handle = store.handle(guid, String::class)

    assertEquals("hello", handle.value)
    assertEquals(1, handle.version)
    assertSame(handle, store.handle(guid, String::class))
  }

  @Test
  fun `a missing asset or loader names what is missing`() {
    val guid = project.asset("a.txt", "hello")

    assertFailsWith<AssetLoadException> { store.handle(guid, String::class) }
    withTextLoader()
    assertFailsWith<AssetLoadException> { store.handle(Guid(7, 7), String::class) }
    assertFailsWith<AssetLoadException> { store.handle(guid, Int::class) }
  }

  @Test
  fun `a file that does not load the first time is an error`() {
    val guid = project.asset("a.txt", "!bad")
    withTextLoader()

    assertFailsWith<AssetLoadException> { store.handle(guid, String::class) }
  }

  @Test
  fun `reloading replaces the value and counts the version`() {
    val guid = project.asset("a.txt", "one")
    withTextLoader()
    val handle = store.handle(guid, String::class)
    Files.writeString(project.path("a.txt"), "two")

    store.reload(guid, ContentHash.of(project.path("a.txt")))

    assertEquals("two", handle.value)
    assertEquals(2, handle.version)
  }

  @Test
  fun `a reload that fails keeps the last good value and reports the error`() {
    val guid = project.asset("a.txt", "one")
    withTextLoader()
    val handle = store.handle(guid, String::class)
    Files.writeString(project.path("a.txt"), "!two")

    store.reload(guid, ContentHash.of(project.path("a.txt")))

    assertEquals("one", handle.value)
    assertEquals(1, handle.version)
    assertEquals("Does not load: broken text", handle.error)
  }

  @Test
  fun `a good reload clears the error`() {
    val guid = project.asset("a.txt", "one")
    withTextLoader()
    val handle = store.handle(guid, String::class)
    Files.writeString(project.path("a.txt"), "!two")
    store.reload(guid, ContentHash.of(project.path("a.txt")))
    Files.writeString(project.path("a.txt"), "three")

    store.reload(guid, ContentHash.of(project.path("a.txt")))

    assertNull(handle.error)
    assertEquals("three", handle.value)
  }

  @Test
  fun `a reload for a version this machine does not have is refused`() {
    val guid = project.asset("a.txt", "one")
    withTextLoader()
    val handle = store.handle(guid, String::class)

    assertFailsWith<AssetOutOfDate> { store.reload(guid, ContentHash.of("other".toByteArray())) }
    assertEquals("one", handle.value)
  }

  @Test
  fun `a reload of a gone file keeps the last value and says so`() {
    val guid = project.asset("a.txt", "one")
    withTextLoader()
    val handle = store.handle(guid, String::class)

    store.reload(guid, GONE_HASH)

    assertEquals("one", handle.value)
    assertEquals("The asset's source file is gone", handle.error)
  }

  @Test
  fun `reloading an asset nobody loaded does nothing`() {
    val guid = project.asset("a.txt", "one")

    store.reload(guid, ContentHash.of("x".toByteArray()))
  }

  private object LengthLoader : AssetLoader<Int> {
    override val type: KClass<Int> = Int::class

    override fun load(guid: Guid, bytes: ByteArray): Int = bytes.size
  }

  private object OtherTextLoader : AssetLoader<String> {
    override val type: KClass<String> = String::class

    override fun load(guid: Guid, bytes: ByteArray): String = "other"
  }

  @Test
  fun `a file that cannot be read marks the handle and refuses the reload`() {
    val guid = project.asset("a.txt", "one")
    withTextLoader()
    val handle = store.handle(guid, String::class)
    val hash = ContentHash.of(project.path("a.txt"))
    Files.delete(project.path("a.txt"))

    assertFailsWith<AssetOutOfDate> { store.reload(guid, hash) }

    assertEquals("one", handle.value)
    assertTrue(handle.error.orEmpty().contains("cannot be read"))
  }

  @Test
  fun `loaders for one extension that produce different types do not tie`() {
    val guid = project.asset("a.txt", "four")
    withTextLoader()
    associations.register(AssetLoader.kind, FileMatch.Extension("txt"), 0, LengthLoader)

    assertEquals("four", store.handle(guid, String::class).value)
    assertEquals(4, store.handle(guid, Int::class).value)
  }

  @Test
  fun `two loaders for one type that tie are a load error naming them`() {
    val guid = project.asset("a.txt", "four")
    withTextLoader()
    associations.register(AssetLoader.kind, FileMatch.Extension("txt"), 0, OtherTextLoader)

    val failure = assertFailsWith<AssetLoadException> { store.handle(guid, String::class) }

    assertTrue(failure.message.orEmpty().contains("a.txt"))
  }
}
