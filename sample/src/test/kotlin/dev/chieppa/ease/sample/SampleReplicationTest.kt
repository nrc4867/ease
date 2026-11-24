package dev.chieppa.ease.sample

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.ecs.replication.Authority
import dev.chieppa.ease.ecs.replication.BitReader
import dev.chieppa.ease.ecs.replication.BitWriter
import dev.chieppa.ease.ecs.replication.DecodeStatus
import dev.chieppa.ease.ecs.replication.NetworkIdentity
import dev.chieppa.ease.ecs.replication.ReplicatedFields
import dev.chieppa.ease.ecs.replication.ReplicatedSerializer
import dev.chieppa.ease.runtime.HostedWorld
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Every component the sample's entities carry replicates, and round-trips through its serializer.
 */
class SampleReplicationTest {
  private val world = HostedWorld(SampleGame(), SampleWorld().fixedStep, input = CourierScript())
  private val writer = BitWriter(CAPACITY)
  private val reader = BitReader()

  /**
   * Sends [entity]'s component through [serializer] and returns the fields that came back wrong.
   */
  private fun <T : Component<T>> wrongFields(
    serializer: ReplicatedSerializer<T>,
    world: World,
    entity: Entity,
  ): Int {
    val sent = serializer.componentOf(world, entity)
    writer.reset()
    serializer.write(writer, sent, ReplicatedFields.allFields(serializer.fieldCount))
    reader.reset(writer.buffer, writer.byteLength)
    val received = serializer.create()
    serializer.read(reader, received)
    assertEquals(DecodeStatus.OK, reader.status, serializer.name)
    return serializer.changedFields(received, sent)
  }

  /** The serial name of the replicated type [component] is, failing when it has none. */
  private fun nameOf(component: Component<*>): String {
    val serializers = world.codec.replicated
    return serializers.single { it.type == component.type() }.name
  }

  @Test
  fun `the sample registers a serializer for every component its entities carry`() {
    repeat(TICKS) { world.step() }
    val serializers = world.codec.replicated
    val replicated = serializers.map { it.name }.toSet()

    val snapshots = world.world.snapshot().values
    val components = snapshots.flatMap { it.components }
    val carried =
      components.filterNot { it is NetworkIdentity || it is Authority }.map { nameOf(it) }.toSet()

    assertEquals(setOf("ease.LocalTransform", "ease.Quad", "sample.Velocity"), carried - COURIER)
    assertTrue(COURIER in carried)
    assertEquals(carried, replicated)
  }

  @Test
  fun `every sample component round-trips, quantized fields to the same step`() {
    repeat(TICKS) { world.step() }
    var checked = 0

    with(world.world) {
      asEntityBag().forEach { entity ->
        world.codec.replicated
          .filter { entity.has(it.type) }
          .forEach { serializer ->
            assertEquals(0, wrongFields(serializer, world.world, entity), serializer.name)
            checked++
          }
      }
    }

    assertEquals(SampleGame.SQUARES.size * SQUARE_TYPES + COURIER_TYPES, checked)
  }

  private companion object {
    const val CAPACITY = 256
    const val TICKS = 45
    const val COURIER = "sample.Courier"
    const val SQUARE_TYPES = 3
    const val COURIER_TYPES = 4
  }
}
