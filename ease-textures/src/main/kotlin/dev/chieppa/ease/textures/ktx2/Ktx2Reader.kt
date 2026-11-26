package dev.chieppa.ease.textures.ktx2

import dev.chieppa.ease.platform.images.TextureSizes
import dev.chieppa.ease.textures.CookedTexture
import dev.chieppa.ease.textures.TextureEncoding
import dev.chieppa.ease.textures.TextureFilter
import dev.chieppa.ease.textures.TextureWrap
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val FORMAT_AT = 12
private const val WIDTH_AT = 20
private const val HEIGHT_AT = 24
private const val DEPTH_AT = 28
private const val LAYERS_AT = 32
private const val FACES_AT = 36
private const val LEVEL_COUNT_AT = 40
private const val SUPERCOMPRESSION_AT = 44
private const val KVD_AT = 56

/**
 * Reads KTX2 files: one 2D image (no layers, faces or depth), without supercompression, in one of
 * the [TextureEncoding]s, with every level present. The sampler's filter and wrap come from the
 * key-value data Ease writes; files without them sample linearly and repeat.
 */
public object Ktx2Reader {
  /** The texture [bytes] hold. Throws [Ktx2Exception] naming what is wrong when it is not one. */
  @Suppress("TooGenericExceptionCaught") // Reads past the end throw IndexOutOfBoundsException.
  public fun read(bytes: ByteArray): CookedTexture =
    try {
      parse(ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN))
    } catch (failure: IndexOutOfBoundsException) {
      throw Ktx2Exception("The file ends early: ${failure.message}", failure)
    } catch (failure: BufferUnderflowException) {
      throw Ktx2Exception("The file ends early", failure)
    } catch (failure: ArithmeticException) {
      throw Ktx2Exception("A level lies outside the file: ${failure.message}", failure)
    }

  private fun parse(buffer: ByteBuffer): CookedTexture {
    val encoding = encodingOf(buffer)
    val width = buffer.getInt(WIDTH_AT)
    val height = buffer.getInt(HEIGHT_AT)
    if (width < 1 || height < 1) throw Ktx2Exception("The texture is $width x $height texels")
    val levelCount = checkShape(buffer, width, height)
    val levels = List(levelCount) { readLevel(buffer, encoding, width, height, it) }
    val values = keyValues(buffer)
    return try {
      CookedTexture(
        width,
        height,
        encoding,
        levels,
        enumValue(values[Ktx2Layout.FILTER_KEY], TextureFilter.LINEAR),
        enumValue(values[Ktx2Layout.WRAP_KEY], TextureWrap.REPEAT),
      )
    } catch (failure: IllegalArgumentException) {
      throw Ktx2Exception(failure.message.orEmpty(), failure)
    }
  }

  /** Level [level]'s bytes, once its length is the one a level of its size in [encoding] has. */
  private fun readLevel(
    buffer: ByteBuffer,
    encoding: TextureEncoding,
    width: Int,
    height: Int,
    level: Int,
  ): ByteArray {
    val entry = Ktx2Layout.LEVEL_INDEX_AT + level * Ktx2Layout.LEVEL_ENTRY_BYTES
    val length = buffer.getLong(entry + Long.SIZE_BYTES)
    val levelWidth = TextureSizes.levelSide(width, level)
    val expected = encoding.levelBytes(levelWidth, TextureSizes.levelSide(height, level))
    if (length != expected) {
      throw Ktx2Exception("Level $level holds $length bytes; a $encoding level holds $expected")
    }
    return slice(buffer, buffer.getLong(entry), length, "level $level")
  }

  /**
   * The [length] bytes at [offset], once both are known to lie inside the file, so a damaged header
   * never makes it allocate more than the file holds.
   */
  private fun slice(buffer: ByteBuffer, offset: Long, length: Long, what: String): ByteArray {
    val inside = offset >= 0 && length >= 0 && offset <= buffer.limit() - length
    if (!inside) throw Ktx2Exception("The $what ($length bytes at $offset) lies outside the file")
    return ByteArray(length.toInt()).also { buffer.get(offset.toInt(), it) }
  }

  /** The encoding the identified file's header names. */
  private fun encodingOf(buffer: ByteBuffer): TextureEncoding {
    val identifier = ByteArray(Ktx2Layout.IDENTIFIER.size).also { buffer.get(0, it) }
    if (!identifier.contentEquals(Ktx2Layout.IDENTIFIER)) throw Ktx2Exception("Not a KTX2 file")
    val vkFormat = buffer.getInt(FORMAT_AT)
    return TextureEncoding.ofVkFormat(vkFormat)
      ?: throw Ktx2Exception("VkFormat $vkFormat is not one Ease reads")
  }

  /**
   * The level count, once the header is known to describe a plain, uncompressed 2D texture with no
   * more levels than a full chain and a level index inside the file.
   */
  private fun checkShape(buffer: ByteBuffer, width: Int, height: Int): Int {
    val plain =
      buffer.getInt(DEPTH_AT) == 0 && buffer.getInt(LAYERS_AT) == 0 && buffer.getInt(FACES_AT) == 1
    if (!plain)
      throw Ktx2Exception("Only plain 2D textures are read; depth, layers or faces are set")
    val levelCount = buffer.getInt(LEVEL_COUNT_AT)
    val supercompressed = buffer.getInt(SUPERCOMPRESSION_AT) != 0
    val fits =
      levelCount in 1..TextureSizes.levelCount(width, height) &&
        Ktx2Layout.LEVEL_INDEX_AT + levelCount * Ktx2Layout.LEVEL_ENTRY_BYTES <= buffer.limit()
    if (!fits || supercompressed) {
      throw Ktx2Exception(
        "Only files with a full chain's levels or fewer, all listed, unsupercompressed, are read"
      )
    }
    return levelCount
  }

  private fun keyValues(buffer: ByteBuffer): Map<String, String> {
    val data =
      slice(
        buffer,
        buffer.getInt(KVD_AT).toLong(),
        buffer.getInt(KVD_AT + 4).toLong(),
        "key-value data",
      )
    val entries = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
    val values = HashMap<String, String>()
    var at = 0
    while (at < data.size) {
      val length = entries.getInt(at).toLong()
      val text = String(slice(entries, at + 4L, length, "key-value entry at $at"), Charsets.UTF_8)
      val pair = text.trimEnd('\u0000')
      val split = pair.indexOf('\u0000')
      if (split > 0) values[pair.substring(0, split)] = pair.substring(split + 1)
      at +=
        Int.SIZE_BYTES + ((length + Int.SIZE_BYTES - 1) / Int.SIZE_BYTES * Int.SIZE_BYTES).toInt()
    }
    return values
  }

  private inline fun <reified E : Enum<E>> enumValue(text: String?, default: E): E =
    if (text == null) {
      default
    } else {
      enumValues<E>().firstOrNull { it.name.equals(text, ignoreCase = true) }
        ?: throw Ktx2Exception("Unknown ${E::class.simpleName} '$text'")
    }
}

/** A file that is not a KTX2 texture Ease reads. */
public class Ktx2Exception(message: String, cause: Throwable? = null) :
  IllegalArgumentException(message, cause)
