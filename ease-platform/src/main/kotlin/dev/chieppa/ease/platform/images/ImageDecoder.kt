package dev.chieppa.ease.platform.images

import java.nio.ByteBuffer
import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil

/** The four bytes every OpenEXR file starts with. */
private val EXR_MAGIC = byteArrayOf(0x76, 0x2f, 0x31, 0x01)

/**
 * Decodes image files from memory: PNG, JPEG, TGA, BMP and Radiance HDR through stb_image, OpenEXR
 * through tinyexr, both LWJGL's prebuilt bindings. 8-bit files give [DecodedImage.Bytes] (16-bit
 * PNGs are reduced to 8 bits), HDR and EXR files [DecodedImage.Floats]. The same bytes give the
 * same pixels every time. For importers in the editor and in cooking; the player loads cooked
 * textures and never decodes.
 */
public object ImageDecoder {
  /** The image [file] holds. Throws [ImageDecodeException] when it is not one. */
  public fun decode(file: ByteArray): DecodedImage {
    if (file.isEmpty()) throw ImageDecodeException("The file is empty")
    val native = MemoryUtil.memAlloc(file.size)
    return try {
      native.put(file).flip()
      when {
        isExr(file) -> ExrDecoder.decode(native)
        STBImage.stbi_is_hdr_from_memory(native) -> decodeHdr(native)
        else -> decodeBytes(native)
      }
    } finally {
      MemoryUtil.memFree(native)
    }
  }

  private fun isExr(file: ByteArray): Boolean =
    file.size >= EXR_MAGIC.size && EXR_MAGIC.indices.all { file[it] == EXR_MAGIC[it] }

  private fun decodeBytes(native: ByteBuffer): DecodedImage.Bytes =
    MemoryStack.stackPush().use { stack ->
      val width = stack.mallocInt(1)
      val height = stack.mallocInt(1)
      val channels = stack.mallocInt(1)
      val pixels =
        STBImage.stbi_load_from_memory(native, width, height, channels, IMAGE_CHANNELS)
          ?: throw ImageDecodeException(failure())
      try {
        val bytes = ByteArray(width[0] * height[0] * IMAGE_CHANNELS)
        pixels.get(0, bytes) // an absolute read: stb frees from the position
        DecodedImage.Bytes(width[0], height[0], bytes)
      } finally {
        STBImage.stbi_image_free(pixels)
      }
    }

  private fun decodeHdr(native: ByteBuffer): DecodedImage.Floats =
    MemoryStack.stackPush().use { stack ->
      val width = stack.mallocInt(1)
      val height = stack.mallocInt(1)
      val channels = stack.mallocInt(1)
      val pixels =
        STBImage.stbi_loadf_from_memory(native, width, height, channels, IMAGE_CHANNELS)
          ?: throw ImageDecodeException(failure())
      try {
        val floats = FloatArray(width[0] * height[0] * IMAGE_CHANNELS)
        pixels.get(0, floats)
        DecodedImage.Floats(width[0], height[0], floats)
      } finally {
        STBImage.stbi_image_free(pixels)
      }
    }

  private fun failure(): String = STBImage.stbi_failure_reason() ?: "stb_image could not decode it"
}
