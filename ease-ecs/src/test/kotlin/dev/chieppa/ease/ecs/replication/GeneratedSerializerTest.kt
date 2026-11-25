package dev.chieppa.ease.ecs.replication

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The serializer KSP writes for [Lantern], through its public interface. */
class GeneratedSerializerTest {
  private val serializer = LanternReplicated
  private val writer = BitWriter(CAPACITY)
  private val reader = BitReader()

  private fun lantern(brightness: Float, fuel: Float, charges: Int, lit: Boolean) =
    Lantern().also {
      it.brightness = brightness
      it.fuel = fuel
      it.charges = charges
      it.lit = lit
    }

  private fun send(value: Lantern, fields: Int, onto: Lantern): Int {
    writer.reset()
    serializer.write(writer, value, fields)
    reader.reset(writer.buffer, writer.byteLength)
    serializer.read(reader, onto)
    assertEquals(DecodeStatus.OK, reader.status)
    return writer.byteLength
  }

  @Test
  fun `the serializer names the class and its options`() {
    assertEquals("dev.chieppa.ease.ecs.replication.Lantern", serializer.name)
    assertEquals(4, serializer.fieldCount)
    assertEquals(0b0001, serializer.interpolatedFields)
    assertEquals(0b0111, serializer.ownerFields)
    assertEquals(0b1011, serializer.othersFields)
  }

  @Test
  fun `every field round-trips, the quantized one within a step`() {
    val sent = lantern(brightness = 0.4f, fuel = -12.5f, charges = -3, lit = true)
    val received = serializer.create()

    send(sent, ALL, received)

    assertEquals(0.4f, received.brightness, 1f / 255)
    assertEquals(-12.5f, received.fuel)
    assertEquals(-3, received.charges)
    assertTrue(received.lit)
  }

  @Test
  fun `a delta carries only the changed fields and lands on the baseline`() {
    val baseline = lantern(brightness = 1f, fuel = 3f, charges = 2, lit = false)
    val value = lantern(brightness = 1f, fuel = 4f, charges = 2, lit = false)
    val replica = serializer.create().also { serializer.copy(baseline, it) }

    val changed = serializer.changedFields(value, baseline)
    val deltaBytes = send(value, changed, replica)
    val fullBytes = send(value, ALL, serializer.create())

    assertEquals(0b0010, changed)
    assertTrue(deltaBytes < fullBytes, "delta $deltaBytes bytes, full $fullBytes")
    assertEquals(4f, replica.fuel)
    assertEquals(2, replica.charges)
  }

  @Test
  fun `a change smaller than a quantization step is no change on the wire, but is a change`() {
    val baseline = lantern(brightness = 0.5f, fuel = 0f, charges = 0, lit = false)
    val value = lantern(brightness = 0.5001f, fuel = 0f, charges = 0, lit = false)

    assertEquals(0, serializer.changedFields(value, baseline))
    assertTrue(serializer.differs(value, baseline))
  }

  @Test
  fun `a float that is not finite is refused`() {
    writer.reset()
    writer.writeBits(0b0010, 4)
    writer.writeBits(Float.NaN.toRawBits(), 32)
    reader.reset(writer.buffer, writer.byteLength)

    serializer.read(reader, serializer.create())

    assertEquals(DecodeStatus.BAD_VALUE, reader.status)
  }

  @Test
  fun `interpolating blends only the interpolated fields, taking the rest from the start`() {
    val into = Lantern()

    serializer.interpolate(lantern(0.2f, 1f, 3, true), lantern(0.6f, 5f, 4, false), 0.25f, into)

    assertEquals(0.3f, into.brightness, BLEND_TOLERANCE)
    assertEquals(1f, into.fuel)
    assertEquals(3, into.charges)
    assertEquals(true, into.lit)
  }

  private companion object {
    const val BLEND_TOLERANCE = 1e-6f
    const val CAPACITY = 64
    const val ALL = 0b1111
  }
}
