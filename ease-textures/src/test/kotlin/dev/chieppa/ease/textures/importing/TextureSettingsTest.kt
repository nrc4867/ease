package dev.chieppa.ease.textures.importing

import dev.chieppa.ease.assets.importing.ImportException
import dev.chieppa.ease.textures.TextureFilter
import dev.chieppa.ease.textures.TextureUsage
import dev.chieppa.ease.textures.TextureWrap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class TextureSettingsTest {
  private fun settings(json: String) = TextureSettings.of(Json.parseToJsonElement(json).jsonObject)

  @Test
  fun `no settings mean an sRGB color texture with mipmaps`() {
    val defaults = TextureSettings.of(JsonObject(emptyMap()))
    assertEquals(TextureUsage.COLOR_SRGB, defaults.usage)
    assertEquals(TextureCompression.AUTO, defaults.compression)
    assertEquals(DEFAULT_MAX_SIZE, defaults.maxSize)
    assertEquals(true, defaults.mipmaps)
  }

  @Test
  fun `every key is read`() {
    val read =
      settings(
        """
        |{"usage":"normal","compression":"bc5","maxSize":512,"mipmaps":false,
        |"filter":"nearest","wrap":"clamp"}
        """
          .trimMargin()
      )
    assertEquals(TextureUsage.NORMAL, read.usage)
    assertEquals(TextureCompression.BC5, read.compression)
    assertEquals(512, read.maxSize)
    assertEquals(false, read.mipmaps)
    assertEquals(TextureFilter.NEAREST, read.filter)
    assertEquals(TextureWrap.CLAMP, read.wrap)
  }

  @Test
  fun `linear color is data`() {
    assertEquals(TextureUsage.DATA_LINEAR, settings("""{"srgb":false}""").usage)
  }

  @Test
  fun `wrong values name their key`() {
    for (bad in
      listOf(
        """{"usage":"paint"}""",
        """{"maxSize":0}""",
        """{"wrap":3}""",
        """{"usage":"data","srgb":true}""",
        """{"mipmaps":"yes"}""",
      )) {
      assertFailsWith<ImportException>(bad) { settings(bad) }
    }
  }
}
