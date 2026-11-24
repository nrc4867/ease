package dev.chieppa.ease.ecs.replication

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Components described at runtime by a schema built in code, as a script would describe one. */
class SchemaSerializerTest {
  private val schema =
    replicatedSchema("test.Lantern") {
      quantized("brightness", Quantization(bits = 8, min = 0f, max = 1f), interpolated = true)
      float("fuel")
      int("charges", audience = Audience.OWNER_ONLY)
      boolean("lit", audience = Audience.ALL_BUT_OWNER)
    }
  private val brightness = schema.indexOf("brightness")
  private val fuel = schema.indexOf("fuel")
  private val charges = schema.indexOf("charges")
  private val lit = schema.indexOf("lit")

  private fun component(level: Float, fuelLeft: Float, count: Int, on: Boolean) =
    schema.component().also {
      it.setFloat(brightness, level)
      it.setFloat(fuel, fuelLeft)
      it.setInt(charges, count)
      it.setBoolean(lit, on)
    }

  private fun bytesOf(write: (BitWriter) -> Unit): ByteArray {
    val writer = BitWriter(CAPACITY)
    write(writer)
    return writer.buffer.copyOf(writer.byteLength)
  }

  @Test
  fun `a schema component round-trips`() {
    val sent = component(0.75f, 9f, 4, on = true)
    val received = schema.component()
    val bytes = bytesOf { schema.serializer.write(it, sent, ALL) }
    val reader = BitReader().apply { reset(bytes, bytes.size) }

    schema.serializer.read(reader, received)

    assertEquals(DecodeStatus.OK, reader.status)
    assertEquals(0.75f, received.getFloat(brightness), 1f / 255)
    assertEquals(9f, received.getFloat(fuel))
    assertEquals(4, received.getInt(charges))
    assertTrue(received.getBoolean(lit))
  }

  @Test
  fun `a schema writes the same bytes as the generated serializer of the same fields`() {
    val described = component(0.3f, 2.5f, 7, on = true)
    val declared =
      Lantern().also {
        it.brightness = 0.3f
        it.fuel = 2.5f
        it.charges = 7
        it.lit = true
      }

    val fromSchema = bytesOf { schema.serializer.write(it, described, ALL) }
    val fromCode = bytesOf { LanternReplicated.write(it, declared, ALL) }

    assertContentEquals(fromCode, fromSchema)
    assertEquals(LanternReplicated.ownerFields, schema.serializer.ownerFields)
    assertEquals(LanternReplicated.othersFields, schema.serializer.othersFields)
    assertEquals(LanternReplicated.interpolatedFields, schema.serializer.interpolatedFields)
  }

  @Test
  fun `a delta names only the changed fields`() {
    val baseline = component(0.5f, 1f, 1, on = false)
    val value = component(0.5f, 1f, 2, on = true)

    assertEquals(0b1100, schema.serializer.changedFields(value, baseline))
  }

  @Test
  fun `a schema refuses what the processor refuses`() {
    assertFailsWith<IllegalArgumentException> { Quantization(bits = 31, min = 0f, max = 1f) }
    assertFailsWith<IllegalArgumentException> { Quantization(bits = 8, min = 1f, max = 1f) }
    assertFailsWith<IllegalArgumentException> {
      replicatedSchema("test.Bad") {
        float("x")
        int("x")
      }
    }
  }

  private companion object {
    const val CAPACITY = 64
    const val ALL = 0b1111
  }
}
