package dev.chieppa.ease.runtime

import dev.chieppa.ease.assets.ContentHash
import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.associations.FileAssociationRegistry
import dev.chieppa.ease.assets.associations.FileMatch
import dev.chieppa.ease.assets.database.AssetDatabase
import dev.chieppa.ease.assets.loading.AssetLoader
import dev.chieppa.ease.assets.loading.AssetStore
import dev.chieppa.ease.assets.meta.AssetMeta
import dev.chieppa.ease.assets.meta.MetaFiles
import dev.chieppa.ease.assets.reload.ReloadAsset
import dev.chieppa.ease.commands.CommandOrigin
import dev.chieppa.ease.commands.CommandRejected
import dev.chieppa.ease.platform.files.FileChangeKind
import dev.chieppa.ease.platform.files.ManualFileWatcher
import dev.chieppa.ease.runtime.loop.FixedStep
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class HostedWorldAssetsTest {
  @TempDir lateinit var folder: Path

  private val watcher = ManualFileWatcher()
  private val guid = Guid(0, 1)

  private object TextLoader : AssetLoader<String> {
    override val type: KClass<String> = String::class

    override fun load(guid: Guid, bytes: ByteArray): String = bytes.decodeToString()
  }

  private fun write(text: String) {
    val file = folder.resolve("greeting.txt")
    Files.writeString(file, text)
    MetaFiles.write(
      MetaFiles.metaOf(file),
      AssetMeta(guid = guid),
    )
  }

  private fun host(): Pair<HostedWorld, dev.chieppa.ease.assets.loading.AssetHandle<String>> {
    write("hello")
    val database = AssetDatabase.load(folder, watcher)
    val associations = FileAssociationRegistry()
    associations.register(AssetLoader.kind, FileMatch.Extension("txt"), 0, TextLoader)
    val store = AssetStore(database, associations)
    val handle = store.handle(guid, String::class)
    return HostedWorld(DriftModule(), FixedStep.perSecond(), assets = store) to handle
  }

  private fun fileChanged() =
    watcher.publish(
      folder.toAbsolutePath().normalize().resolve("greeting.txt"),
      FileChangeKind.MODIFIED,
    )

  @Test
  fun `a changed file on disk reloads in the running world at the next tick`() {
    val (hosted, handle) = host()
    hosted.step()

    Files.writeString(folder.resolve("greeting.txt"), "hello again")
    fileChanged()

    assertEquals("hello", handle.value)
    hosted.step()
    assertEquals("hello again", handle.value)
    assertEquals(2, handle.version)
  }

  @Test
  fun `the reload is a logged command stamped with the tick it ran on`() {
    val (hosted, _) = host()
    hosted.step()
    hosted.step()
    Files.writeString(folder.resolve("greeting.txt"), "changed")
    fileChanged()

    hosted.step()

    val log = hosted.history.log
    val entry = log.all.last()
    val command = log.commandOf(entry) as ReloadAsset
    assertEquals(2, entry.tick)
    assertEquals(CommandOrigin.GAME, entry.origin)
    assertEquals(guid, command.asset)
    assertEquals(ContentHash.of(folder.resolve("greeting.txt")), command.contentHash)
  }

  @Test
  fun `a tick with no file change logs no reload`() {
    val (hosted, _) = host()
    val log = hosted.history.log
    val before = log.all.size

    repeat(3) { hosted.step() }

    assertEquals(before, log.all.size)
  }

  @Test
  fun `a command from elsewhere reloads too, and is refused for a file this machine lacks`() {
    val (hosted, handle) = host()
    Files.writeString(folder.resolve("greeting.txt"), "from disk")
    val hash = ContentHash.of(folder.resolve("greeting.txt"))

    hosted.execute(ReloadAsset(guid, hash), CommandOrigin.NETWORK)

    assertEquals("from disk", handle.value)
    assertFailsWith<CommandRejected> {
      hosted.execute(ReloadAsset(guid, ContentHash.of("other".toByteArray())))
    }
    assertEquals("from disk", handle.value)
  }

  @Test
  fun `the reload command survives the codec`() {
    val (hosted, _) = host()
    val command = ReloadAsset(guid, "sha256:abc")

    val back = hosted.codec.commandFromBinary(hosted.codec.commandToBinary(command)) as ReloadAsset
    val fromJson = hosted.codec.commandFromJson(hosted.codec.commandToJson(command)) as ReloadAsset

    assertEquals(guid, back.asset)
    assertEquals("sha256:abc", fromJson.contentHash)
    assertTrue(hosted.codec.commandToJson(command).contains("ease.ReloadAsset"))
  }
}
