package dev.chieppa.ease.net.snapshot

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Family
import com.github.quillraven.fleks.World
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.replication.replication
import kotlin.math.sqrt

private const val INITIAL_ENTITIES = 64

/** Floats a position takes: x, y and z. */
private const val AXES = 3

/**
 * How far each entity's `LocalTransform` moved since the last tick, for priority: measured once per
 * tick for the whole world and read by every peer. An entity new under its id, or without a
 * transform the tick before, has not moved. It allocates only when an entity id is first seen.
 */
internal class EntityMotion(private val world: World, private val entities: Family) {
  private val replication = world.replication
  private var networkIds = LongArray(INITIAL_ENTITIES)
  private var positions = FloatArray(INITIAL_ENTITIES * AXES) { Float.NaN }
  private var speeds = FloatArray(INITIAL_ENTITIES)

  /** Measures every entity's movement since the last call. */
  fun update() {
    entities.forEach { visit(it) }
  }

  /** How far [entity] moved in the last tick measured. */
  fun speedOf(entity: Entity): Float = if (entity.id < speeds.size) speeds[entity.id] else 0f

  private fun visit(entity: Entity) {
    val id = entity.id
    if (id >= speeds.size) grow(id)
    val networkId = replication.networkIdOf(entity).value
    val fresh = networkIds[id] != networkId
    networkIds[id] = networkId
    val at = id * AXES
    val transform = with(world) { entity.getOrNull(LocalTransform) }
    if (transform == null) {
      positions[at] = Float.NaN
      speeds[id] = 0f
      return
    }
    val dx = transform.x - positions[at]
    val dy = transform.y - positions[at + 1]
    val dz = transform.z - positions[at + 2]
    val moved = sqrt(dx * dx + dy * dy + dz * dz)
    speeds[id] = if (fresh || moved.isNaN()) 0f else moved
    positions[at] = transform.x
    positions[at + 1] = transform.y
    positions[at + 2] = transform.z
  }

  private fun grow(id: Int) {
    val size = maxOf(id + 1, speeds.size * 2)
    val oldPositions = positions.size
    networkIds = networkIds.copyOf(size)
    speeds = speeds.copyOf(size)
    positions = positions.copyOf(size * AXES)
    positions.fill(Float.NaN, oldPositions, positions.size)
  }
}
