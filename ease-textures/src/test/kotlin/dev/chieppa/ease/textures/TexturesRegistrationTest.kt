package dev.chieppa.ease.textures

import dev.chieppa.ease.assets.associations.FileAssociationRegistry
import dev.chieppa.ease.assets.database.AssetDatabase
import dev.chieppa.ease.assets.database.AssetProblem
import dev.chieppa.ease.assets.importing.ImportService
import dev.chieppa.ease.assets.loading.AssetStore
import dev.chieppa.ease.platform.files.ManualFileWatcher
import dev.chieppa.ease.textures.importing.COOKED_TEXTURE_NAME
import dev.chieppa.ease.textures.ktx2.Ktx2Writer
import dev.chieppa.ease.textures.loading.TextureLoader
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.io.TempDir

class TexturesRegistrationTest {
  @TempDir lateinit var folder: Path

  private val associations = FileAssociationRegistry().also { it.registerTextures() }

  private fun open(): AssetDatabase {
    val database = AssetDatabase.open(folder, ManualFileWatcher(), associations = associations)
    database.problems.filterIsInstance<AssetProblem.SourceWithoutMeta>().forEach {
      database.adopt(it.source)
    }
    return database
  }

  @Test
  fun `an image in the content imports to a KTX2 texture the loader reads`() {
    ImageIO.write(
      BufferedImage(8, 4, BufferedImage.TYPE_INT_ARGB),
      "png",
      folder.resolve("tile.png").toFile(),
    )
    val database = open()
    val report = ImportService(database, associations).use { it.refresh() }
    assertEquals(listOf(Path.of("tile.png")), report.imported)
    val guid = checkNotNull(database.at(Path.of("tile.png"))).guid
    val cooked = ImportService(database, associations).use { it.cooked(guid) }.single()
    assertEquals(COOKED_TEXTURE_NAME, cooked.name)
    val texture = TextureLoader.load(guid, cooked.bytes)
    assertEquals(8 to 4, texture.width to texture.height)
    assertEquals(TextureEncoding.BC7_SRGB, texture.encoding)
  }

  @Test
  fun `a KTX2 file in the content loads as a texture`() {
    val written =
      CookedTexture(2, 2, TextureEncoding.RGBA8_UNORM, listOf(ByteArray(16), ByteArray(4)))
    Files.write(folder.resolve("ready.ktx2"), Ktx2Writer.write(written))
    val database = open()
    val guid = checkNotNull(database.at(Path.of("ready.ktx2"))).guid

    val handle = AssetStore(database, associations).handle(guid, CookedTexture::class)

    assertEquals(2, handle.value.levels.size)
  }
}
