package dev.chieppa.ease.ecs.replication

import dev.chieppa.ease.testing.AllocationBudget
import kotlin.test.Test
import kotlin.test.assertEquals

/** The hot replication paths allocate nothing once warm. */
class ReplicationAllocationTest {
  private val pair = ReplicaPair(ENTITIES)

  @Test
  fun `a guarded write allocates nothing`() {
    val replication = pair.authority.replication
    val entity = pair.held[0]

    AllocationBudget.assertWithin(
      0,
      { replication.write(entity, Lantern) { fuel += 1f } },
      WARMUP,
      MEASURED,
    )
  }

  @Test
  fun `a tick with the end-of-tick check allocates nothing`() {
    AllocationBudget.assertWithin(0, { pair.authority.update(DELTA) }, WARMUP, MEASURED)
  }

  @Test
  fun `writing a state packet into a reused buffer allocates nothing`() {
    AllocationBudget.assertWithin(0, { pair.writeAll() }, WARMUP, MEASURED)
  }

  @Test
  fun `reading a state packet into the replica allocates nothing`() {
    val length = pair.writeAll()
    val bytes = pair.writer.buffer
    assertEquals(DecodeStatus.OK, pair.reader.read(bytes, length))

    AllocationBudget.assertWithin(0, { pair.reader.read(bytes, length) }, WARMUP, MEASURED)
  }

  private companion object {
    const val ENTITIES = 64
    const val WARMUP = 1_000
    const val MEASURED = 1_000
    const val DELTA = 1f / 60f
  }
}
