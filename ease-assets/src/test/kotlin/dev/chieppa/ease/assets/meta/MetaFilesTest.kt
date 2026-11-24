package dev.chieppa.ease.assets.meta

import dev.chieppa.ease.assets.Guid
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class MetaFilesTest {
  @TempDir lateinit var folder: Path

  private val meta =
    AssetMeta(
      guid = Guid.parse("3f2b8c1e-5d4a-4e7b-9a60-1c2d3e4f5a6b"),
      contentHash = "sha256:ab12cd",
    )

  @Test
  fun `a meta file sits beside its source and names it`() {
    val source = folder.resolve("art").resolve("rock.png")

    assertEquals(folder.resolve("art").resolve("rock.png.meta"), MetaFiles.metaOf(source))
    assertEquals(source, MetaFiles.sourceOf(MetaFiles.metaOf(source)))
    assertTrue(MetaFiles.isMeta(MetaFiles.metaOf(source)))
    assertFalse(MetaFiles.isMeta(source))
  }

  @Test
  fun `a meta file is canonical YAML with sorted keys`() {
    val text = MetaFiles.encode(meta)

    assertEquals(
      "contentHash: \"sha256:ab12cd\"\n" +
        "formatVersion: 1\n" +
        "guid: \"3f2b8c1e-5d4a-4e7b-9a60-1c2d3e4f5a6b\"\n",
      text,
    )
  }

  @Test
  fun `what is written reads back`() {
    val file = folder.resolve("a.meta")
    MetaFiles.write(file, meta)

    val back = MetaFiles.read(file)

    assertEquals(meta.guid, back.guid)
    assertEquals(meta.contentHash, back.contentHash)
  }

  @Test
  fun `writing the same content again leaves the file alone`() {
    val file = folder.resolve("a.meta")

    assertTrue(MetaFiles.write(file, meta))
    assertFalse(MetaFiles.write(file, meta))
    assertTrue(MetaFiles.write(file, AssetMeta(guid = meta.guid, contentHash = "ffff")))
  }

  @Test
  fun `a meta file from a newer Ease is refused, naming the file`() {
    val file = folder.resolve("future.meta")
    Files.writeString(file, "contentHash: x\nformatVersion: 99\nguid: \"${meta.guid}\"\n")

    val failure = assertFailsWith<MetaFileException> { MetaFiles.read(file) }

    assertTrue(failure.message.orEmpty().contains("future.meta"))
  }

  @Test
  fun `text that is not a meta file is refused, naming the file`() {
    val file = folder.resolve("broken.meta")
    Files.writeString(file, "formatVersion: 1\nguid: not-a-guid\ncontentHash: x\n")

    val failure = assertFailsWith<MetaFileException> { MetaFiles.read(file) }

    assertTrue(failure.message.orEmpty().contains("broken.meta"))
  }
}
