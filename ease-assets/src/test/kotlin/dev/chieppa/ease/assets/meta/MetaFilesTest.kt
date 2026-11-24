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

  private val meta = AssetMeta(guid = Guid.parse("3f2b8c1e-5d4a-4e7b-9a60-1c2d3e4f5a6b"))

  @Test
  fun `a meta file sits beside its source and names it`() {
    val source = folder.resolve("art").resolve("rock.png")

    assertEquals(folder.resolve("art").resolve("rock.png.meta"), MetaFiles.metaOf(source))
    assertEquals(source, MetaFiles.sourceOf(MetaFiles.metaOf(source)))
    assertTrue(MetaFiles.isMeta(MetaFiles.metaOf(source)))
    assertFalse(MetaFiles.isMeta(source))
  }

  @Test
  fun `a meta file holds only its format version and GUID, as canonical YAML`() {
    val text = MetaFiles.encode(meta)

    assertEquals("formatVersion: 1\nguid: \"3f2b8c1e-5d4a-4e7b-9a60-1c2d3e4f5a6b\"\n", text)
  }

  @Test
  fun `what is written reads back`() {
    val file = folder.resolve("a.meta")
    MetaFiles.write(file, meta)

    assertEquals(meta.guid, MetaFiles.read(file).guid)
  }

  @Test
  fun `writing the same content again leaves the file alone`() {
    val file = folder.resolve("a.meta")

    assertTrue(MetaFiles.write(file, meta))
    assertFalse(MetaFiles.write(file, meta))
    assertTrue(MetaFiles.write(file, AssetMeta(guid = Guid(1, 2))))
  }

  @Test
  fun `a meta file from a newer Ease is refused, naming the file`() {
    val file = folder.resolve("future.meta")
    Files.writeString(file, "formatVersion: 99\nguid: \"${meta.guid}\"\n")

    val failure = assertFailsWith<MetaFileException> { MetaFiles.read(file) }

    assertTrue(failure.message.orEmpty().contains("future.meta"))
  }

  @Test
  fun `text that is not a meta file is refused, naming the file`() {
    val file = folder.resolve("broken.meta")
    Files.writeString(file, "formatVersion: 1\nguid: not-a-guid\n")

    val failure = assertFailsWith<MetaFileException> { MetaFiles.read(file) }

    assertTrue(failure.message.orEmpty().contains("broken.meta"))
  }

  @Test
  fun `a meta file that cannot be read as text is refused, naming the file`() {
    val file = folder.resolve("binary.meta")
    Files.write(file, byteArrayOf(0x66, 0x3a, 0x20, -1, -2, -3))

    val failure = assertFailsWith<MetaFileException> { MetaFiles.read(file) }

    assertTrue(failure.message.orEmpty().contains("binary.meta"))
  }

  @Test
  fun `a meta file that is missing is refused, naming the file`() {
    val failure = assertFailsWith<MetaFileException> { MetaFiles.read(folder.resolve("none.meta")) }

    assertTrue(failure.message.orEmpty().contains("none.meta"))
  }
}
