package dev.chieppa.ease.ecs.replication

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Random, truncated and corrupted packets fed to a replica: reading never throws, refuses with a
 * status instead, and a refused packet leaves the replica as it was. The generator is seeded, so
 * every run feeds the same bytes.
 */
class HostilePacketsTest {
  private val pair = ReplicaPair(ENTITIES)
  private val random = Random(SEED)

  private fun lengthOfValidPacket(): Int {
    pair.held.forEach { entity ->
      pair.authority.replication.write(entity, Lantern) {
        fuel = random.nextFloat() * RANGE
        charges = random.nextInt()
        lit = random.nextBoolean()
        brightness = random.nextFloat()
      }
    }
    return pair.writeAll()
  }

  /** Reads [bytes] and checks the replica is unchanged when they are refused. */
  private fun feed(bytes: ByteArray, length: Int = bytes.size): DecodeStatus {
    val before = pair.replicaState()
    val status = pair.reader.read(bytes, length)
    if (status != DecodeStatus.OK) assertEquals(before, pair.replicaState(), "after $status")
    return status
  }

  @Test
  fun `random bytes of every length are refused or applied, never thrown`() {
    repeat(RANDOM_PACKETS) {
      val bytes = random.nextBytes(random.nextInt(StatePackets.MAX_BYTES + EXTRA))
      feed(bytes)
    }
  }

  @Test
  fun `every truncation of a valid packet is refused`() {
    val length = lengthOfValidPacket()
    val valid = pair.writer.buffer.copyOf(length)

    for (cut in 0 until length) {
      assertNotEquals(DecodeStatus.OK, feed(valid, cut), "cut at $cut of $length")
    }
    assertEquals(DecodeStatus.OK, feed(valid))
  }

  @Test
  fun `flipped bits in valid packets are refused or applied, never thrown`() {
    repeat(CORRUPTED_PACKETS) {
      val length = lengthOfValidPacket()
      val bytes = pair.writer.buffer.copyOf(length)
      repeat(1 + random.nextInt(MAX_FLIPS)) {
        val bit = random.nextInt(length * BYTE_BITS)
        val index = bit / BYTE_BITS
        bytes[index] = (bytes[index].toInt() xor (1 shl (bit % BYTE_BITS))).toByte()
      }
      feed(bytes)
    }
  }

  @Test
  fun `received state keeps passing the replica's end-of-tick check`() {
    repeat(CORRUPTED_PACKETS) {
      feed(pair.writer.buffer.copyOf(lengthOfValidPacket()))
      pair.replica.update(DELTA)
    }
  }

  private companion object {
    const val SEED = 2_026L
    const val ENTITIES = 8
    const val RANDOM_PACKETS = 5_000
    const val CORRUPTED_PACKETS = 2_000
    const val MAX_FLIPS = 4
    const val EXTRA = 64
    const val BYTE_BITS = 8
    const val RANGE = 100f
    const val DELTA = 1f / 60f
  }
}
