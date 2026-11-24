package dev.chieppa.ease.commands.wire

import dev.chieppa.ease.ecs.replication.BitReader
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.SerializersModule

private const val BYTE_BITS = 8
private const val SHORT_BITS = 16
private const val CHAR_BITS = 16
private const val HALF_BITS = 32
private const val HALF_MASK = 0xFFFF_FFFFL

/**
 * Reads what a [WireEncoder] wrote, treating every bit as hostile: sizes, lengths, enum indexes and
 * subtype ids are bounded before they are used, nesting is limited, and floats must be finite. The
 * first problem throws [WireRefused], which the caller turns into a refusal. It allocates nothing
 * itself; what the values' own serializers build is the decoded value.
 */
// One override per kind of value kotlinx serialization asks a format for.
@Suppress("TooManyFunctions")
@OptIn(ExperimentalSerializationApi::class)
internal class WireDecoder(
  override val serializersModule: SerializersModule,
  private val tables: List<WireTable>,
) : AbstractDecoder() {
  /** The bits being read; reset it before each value. */
  val reader: BitReader = BitReader()
  private val chars = StringBuilder()
  private var depth = 0

  /** Reads one value with [deserializer] from [reader]'s bits. */
  fun <T> read(deserializer: DeserializationStrategy<T>): T {
    depth = 0
    return decodeSerializableValue(deserializer)
  }

  private fun refuse(detail: String): Nothing = throw WireRefused(detail)

  private fun checked(): BitReader {
    if (!reader.ok) refuse("The bits end before the value does")
    return reader
  }

  override fun decodeSequentially(): Boolean = true

  override fun decodeElementIndex(descriptor: SerialDescriptor): Int = CompositeDecoder.DECODE_DONE

  override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
    depth++
    if (depth > Wire.MAX_DEPTH) refuse("Values nest deeper than ${Wire.MAX_DEPTH}")
    return this
  }

  override fun endStructure(descriptor: SerialDescriptor) {
    depth--
  }

  override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
    val size = unsigned()
    if (size > Wire.MAX_COLLECTION || size > reader.remainingBits) {
      refuse("A collection of $size elements is longer than its bits allow")
    }
    return size.toInt()
  }

  override fun decodeNotNullMark(): Boolean = bit()

  override fun decodeNull(): Nothing? = null

  override fun decodeBoolean(): Boolean = bit()

  override fun decodeByte(): Byte = bits(BYTE_BITS).toByte()

  override fun decodeShort(): Short = bits(SHORT_BITS).toShort()

  override fun decodeChar(): Char = bits(CHAR_BITS).toChar()

  override fun decodeInt(): Int {
    val value = unzigzag(unsigned())
    if (value < Int.MIN_VALUE || value > Int.MAX_VALUE) refuse("$value does not fit an Int")
    return value.toInt()
  }

  override fun decodeLong(): Long {
    val value = reader.readUnsigned()
    checked()
    return unzigzag(value)
  }

  override fun decodeFloat(): Float {
    val value = Float.fromBits(bits(Float.SIZE_BITS))
    if (!value.isFinite()) refuse("A float that is not finite")
    return value
  }

  override fun decodeDouble(): Double {
    val low = bits(HALF_BITS).toLong() and HALF_MASK
    val high = bits(HALF_BITS).toLong() and HALF_MASK
    val value = Double.fromBits(low or (high shl HALF_BITS))
    if (!value.isFinite()) refuse("A double that is not finite")
    return value
  }

  override fun decodeString(): String {
    val length = unsigned()
    if (length > reader.remainingBits / BYTE_BITS) refuse("A string longer than its bits allow")
    chars.setLength(0)
    repeat(length.toInt()) {
      val code = unsigned()
      if (code > Char.MAX_VALUE.code) refuse("Character $code is outside UTF-16")
      chars.append(code.toInt().toChar())
    }
    return chars.toString()
  }

  override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
    val index = unsigned()
    if (index >= enumDescriptor.elementsCount) {
      refuse("${enumDescriptor.serialName} has no entry $index")
    }
    return index.toInt()
  }

  override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
    val table = if (deserializer is PolymorphicSerializer<*>) tableOf(deserializer) else null
    if (table == null) return deserializer.deserialize(this)
    val id = unsigned()
    if (id >= table.size) refuse("No ${table.base.simpleName} has wire id $id")
    @Suppress("UNCHECKED_CAST") // Every subtype in the table is a T.
    val subtype = table.serializerAt(id.toInt()) as DeserializationStrategy<T>
    return subtype.deserialize(this)
  }

  private fun tableOf(serializer: PolymorphicSerializer<*>): WireTable? {
    val base = serializer.baseClass.java
    for (index in tables.indices) if (tables[index].base == base) return tables[index]
    return null
  }

  private fun bit(): Boolean {
    val value = reader.readBoolean()
    checked()
    return value
  }

  private fun bits(count: Int): Int {
    val value = reader.readBits(count)
    checked()
    return value
  }

  private fun unsigned(): Long {
    val value = reader.readUnsigned()
    checked()
    if (value < 0) refuse("A number past the largest a value may hold")
    return value
  }
}
