package dev.chieppa.ease.animation.assets

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Little-endian writes of the cooked animation formats: ints, floats, strings, float arrays. */
internal class BinaryOut {
  private val bytes = ByteArrayOutputStream()
  private val scratch = ByteBuffer.allocate(Int.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)

  fun int(value: Int): BinaryOut = apply {
    scratch.clear()
    bytes.write(scratch.putInt(value).array(), 0, Int.SIZE_BYTES)
  }

  fun float(value: Float): BinaryOut = int(value.toRawBits())

  fun string(value: String): BinaryOut = apply {
    val utf8 = value.toByteArray(Charsets.UTF_8)
    int(utf8.size)
    bytes.write(utf8)
  }

  fun floats(values: FloatArray): BinaryOut = apply {
    int(values.size)
    values.forEach { float(it) }
  }

  fun toByteArray(): ByteArray = bytes.toByteArray()
}

/** Reads what [BinaryOut] wrote, failing on bytes that run short. */
internal class BinaryIn(bytes: ByteArray) {
  private val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

  fun int(): Int = buffer.getInt()

  fun float(): Float = buffer.getFloat()

  fun string(): String {
    val utf8 = ByteArray(count())
    buffer.get(utf8)
    return utf8.toString(Charsets.UTF_8)
  }

  fun floats(): FloatArray = FloatArray(count()) { buffer.getFloat() }

  /** A count, checked against the bytes left, so a broken file cannot ask for a huge array. */
  fun count(): Int {
    val count = int()
    require(count in 0..buffer.remaining()) { "a count of $count runs past the file's end" }
    return count
  }
}
