package dev.chieppa.ease.commands.wire

import dev.chieppa.ease.ecs.replication.BitWriter
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.SerializersModule

private const val BYTE_BITS = 8
private const val SHORT_BITS = 16
private const val CHAR_BITS = 16
private const val HALF_BITS = 32

/**
 * Writes `@Serializable` values in the compact wire form into a reused [BitWriter], allocating
 * nothing itself: no field names, every field in declaration order, defaults included; integers as
 * zigzag variable-length numbers, floats and doubles as their bits, booleans and null marks as one
 * bit, strings as a length and their characters, collections as a size and their elements. A value
 * of a polymorphic base in [tables] is its subtype's wire id, then the value.
 */
// One override per kind of value kotlinx serialization asks a format for.
@Suppress("TooManyFunctions")
@OptIn(ExperimentalSerializationApi::class)
internal class WireEncoder(
  override val serializersModule: SerializersModule,
  private val tables: List<WireTable>,
) : AbstractEncoder() {
  private var target: BitWriter? = null

  /** Writes [value] with [serializer] into [writer], after what it already holds. */
  fun <T> write(serializer: SerializationStrategy<T>, value: T, writer: BitWriter) {
    target = writer
    try {
      encodeSerializableValue(serializer, value)
    } finally {
      target = null
    }
  }

  private fun out(): BitWriter = checkNotNull(target) { "Write through WireEncoder.write" }

  override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean = true

  override fun encodeBoolean(value: Boolean) = out().writeBoolean(value)

  override fun encodeByte(value: Byte) = out().writeBits(value.toInt(), BYTE_BITS)

  override fun encodeShort(value: Short) = out().writeBits(value.toInt(), SHORT_BITS)

  override fun encodeChar(value: Char) = out().writeBits(value.code, CHAR_BITS)

  override fun encodeInt(value: Int) = out().writeUnsigned(zigzag(value.toLong()))

  override fun encodeLong(value: Long) = out().writeUnsigned(zigzag(value))

  override fun encodeFloat(value: Float) = out().writeBits(value.toRawBits(), Float.SIZE_BITS)

  override fun encodeDouble(value: Double) {
    val bits = value.toRawBits()
    out().writeBits(bits.toInt(), HALF_BITS)
    out().writeBits((bits ushr HALF_BITS).toInt(), HALF_BITS)
  }

  override fun encodeString(value: String) {
    val writer = out()
    writer.writeUnsigned(value.length.toLong())
    for (index in value.indices) writer.writeUnsigned(value[index].code.toLong())
  }

  override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) =
    out().writeUnsigned(index.toLong())

  override fun encodeNull() = out().writeBoolean(false)

  override fun encodeNotNullMark() = out().writeBoolean(true)

  override fun beginCollection(
    descriptor: SerialDescriptor,
    collectionSize: Int,
  ): CompositeEncoder {
    out().writeUnsigned(collectionSize.toLong())
    return this
  }

  override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
    val table = if (serializer is PolymorphicSerializer<*>) tableOf(serializer) else null
    if (table == null || value == null) {
      serializer.serialize(this, value)
      return
    }
    val id = table.idOf(value)
    if (id == WireTable.NONE) {
      throw SerializationException("${value.javaClass.name} is not registered as a subtype")
    }
    out().writeUnsigned(id.toLong())
    @Suppress("UNCHECKED_CAST") // The table holds the serializer of value's own class.
    (table.serializerAt(id) as SerializationStrategy<T>).serialize(this, value)
  }

  private fun tableOf(serializer: PolymorphicSerializer<*>): WireTable? {
    val base = serializer.baseClass.java
    for (index in tables.indices) if (tables[index].base == base) return tables[index]
    return null
  }
}
