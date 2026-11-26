package dev.chieppa.ease.platform.images

import java.nio.ByteBuffer
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.tinyexr.EXRHeader
import org.lwjgl.util.tinyexr.EXRImage
import org.lwjgl.util.tinyexr.EXRVersion
import org.lwjgl.util.tinyexr.TinyEXR

/** The channel names read, in output order, and the value a missing one takes. */
private val CHANNELS = listOf("R" to 0f, "G" to 0f, "B" to 0f, "A" to 1f)

/** The channels an image needs at least one of. */
private val COLOR_NAMES = setOf("R", "G", "B", "Y")

/**
 * Reads single-part, scanline OpenEXR files through tinyexr into linear float RGBA. Channels match
 * by the name after their layer, ignoring case; a file with only a `Y` channel is gray, one with
 * none of R, G, B or Y is refused, and other channels are ignored.
 */
internal object ExrDecoder {
  fun decode(native: ByteBuffer): DecodedImage.Floats =
    MemoryStack.stackPush().use { stack ->
      val version = EXRVersion.malloc(stack)
      check(TinyEXR.ParseEXRVersionFromMemory(version, native), "version", null)
      if (version.multipart() || version.tiled()) {
        throw ImageDecodeException("Only single-part scanline EXR files are read")
      }
      val header = EXRHeader.malloc(stack)
      TinyEXR.InitEXRHeader(header)
      val error = stack.mallocPointer(1)
      check(TinyEXR.ParseEXRHeaderFromMemory(header, version, native, error), "header", error)
      try {
        val types = header.requested_pixel_types()
        for (channel in 0 until header.num_channels()) {
          types.put(channel, TinyEXR.TINYEXR_PIXELTYPE_FLOAT)
        }
        loadImage(stack, header, native)
      } finally {
        TinyEXR.FreeEXRHeader(header)
      }
    }

  private fun loadImage(stack: MemoryStack, header: EXRHeader, native: ByteBuffer) =
    EXRImage.malloc(stack).let { image ->
      TinyEXR.InitEXRImage(image)
      val error = stack.mallocPointer(1)
      check(TinyEXR.LoadEXRImageFromMemory(image, header, native, error), "image", error)
      try {
        planesToRgba(header, image)
      } finally {
        TinyEXR.FreeEXRImage(image)
      }
    }

  private fun planesToRgba(header: EXRHeader, image: EXRImage): DecodedImage.Floats {
    val width = image.width()
    val height = image.height()
    val count = width * height
    val names = channelNames(header)
    if (names.none { it in COLOR_NAMES }) {
      throw ImageDecodeException("The EXR file has no R, G, B or Y channel")
    }
    val planes = image.images() ?: throw ImageDecodeException("The EXR image has no pixels")
    val gray = names.indexOf("Y")
    val out = FloatArray(count * IMAGE_CHANNELS)
    CHANNELS.forEachIndexed { slot, (name, missing) ->
      val index = names.indexOf(name).takeIf { it >= 0 } ?: gray.takeIf { slot < 3 } ?: -1
      if (index < 0) {
        for (pixel in 0 until count) out[pixel * IMAGE_CHANNELS + slot] = missing
      } else {
        val plane = MemoryUtil.memFloatBuffer(planes[index], count)
        for (pixel in 0 until count) out[pixel * IMAGE_CHANNELS + slot] = plane[pixel]
      }
    }
    return DecodedImage.Floats(width, height, out)
  }

  /**
   * Each channel's name without its layer, upper case: `diffuse.r` and `R` are both `R`. A file
   * with several layers takes each channel from the first layer that has it.
   */
  private fun channelNames(header: EXRHeader): List<String> =
    header.channels().let { infos ->
      List(header.num_channels()) { infos[it].nameString().substringAfterLast('.').uppercase() }
    }

  /** Throws when [status] is not success, with tinyexr's message, which it then frees. */
  private fun check(status: Int, stage: String, error: PointerBuffer?) {
    if (status == TinyEXR.TINYEXR_SUCCESS) return
    val message =
      error
        ?.get(0)
        ?.takeIf { it != 0L }
        ?.let { pointer ->
          MemoryUtil.memUTF8(pointer).also { TinyEXR.nFreeEXRErrorMessage(pointer) }
        }
    throw ImageDecodeException("The EXR $stage does not read (${message ?: "status $status"})")
  }
}
