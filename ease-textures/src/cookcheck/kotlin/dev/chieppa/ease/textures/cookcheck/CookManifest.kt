package dev.chieppa.ease.textures.cookcheck

import dev.chieppa.ease.assets.ContentHash
import dev.chieppa.ease.platform.images.DecodedImage
import dev.chieppa.ease.platform.images.ImageDecoder
import dev.chieppa.ease.textures.importing.TextureCooker
import dev.chieppa.ease.textures.importing.TextureImporter
import dev.chieppa.ease.textures.importing.TextureSettings
import dev.chieppa.ease.textures.ktx2.Ktx2Writer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/** The settings every source is cooked under, by name. */
private val VARIANTS =
  linkedMapOf(
    "color" to "{}",
    "data" to """{"usage":"data"}""",
    "normal" to """{"usage":"normal"}""",
    "rgba8" to """{"compression":"rgba8"}""",
    "rgba16f" to """{"compression":"rgba16f","filter":"nearest","wrap":"clamp"}""",
    "small" to """{"maxSize":32,"mipmaps":false}""",
  )

private const val SYNTHETIC_SIZE = 20
private const val FLOAT_SCALE = 4f

/**
 * Cooks every image in a folder (args[0]) plus a synthetic HDR image under each of [VARIANTS], and
 * writes one line per result to args[1]: the cooked bytes' hash, the source and the variant. CI
 * runs it on Windows, Linux and macOS and fails when the files differ, so cooked bytes stay
 * identical on every OS.
 */
public fun main(args: Array<String>) {
  require(args.size == 2) { "Usage: CookManifest <image folder> <output file>" }
  val sources =
    Files.list(Path.of(args[0])).use { files ->
      files.filter { it.extension.lowercase() in TextureImporter.extensions }.sorted().toList()
    }
  val images = sources.associate { it.name to ImageDecoder.decode(Files.readAllBytes(it)) }
  val lines = mutableListOf<String>()
  for ((name, image) in images + ("synthetic.hdr" to synthetic())) {
    for ((variant, json) in VARIANTS) {
      val settings = TextureSettings.of(Json.parseToJsonElement(json).jsonObject)
      val bytes = Ktx2Writer.write(TextureCooker.cook(image, settings))
      lines += "${ContentHash.of(bytes)}  $name  $variant"
    }
  }
  // Newlines are written as LF on every OS, so the files compare byte for byte.
  Files.writeString(Path.of(args[1]), lines.joinToString("\n", postfix = "\n"))
  lines.forEach(::println)
}

/** A high dynamic range image whose values span several powers of two. */
private fun synthetic(): DecodedImage.Floats {
  val rgba = FloatArray(SYNTHETIC_SIZE * SYNTHETIC_SIZE * 4)
  for (pixel in 0 until SYNTHETIC_SIZE * SYNTHETIC_SIZE) {
    val x = pixel % SYNTHETIC_SIZE
    val y = pixel / SYNTHETIC_SIZE
    rgba[pixel * 4] = x * FLOAT_SCALE / SYNTHETIC_SIZE
    rgba[pixel * 4 + 1] = 1f / (1 + y)
    rgba[pixel * 4 + 2] = (x * y).toFloat() / SYNTHETIC_SIZE
    rgba[pixel * 4 + 3] = 1f
  }
  return DecodedImage.Floats(SYNTHETIC_SIZE, SYNTHETIC_SIZE, rgba)
}
