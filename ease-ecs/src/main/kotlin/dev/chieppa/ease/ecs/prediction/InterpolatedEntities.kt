package dev.chieppa.ease.ecs.prediction

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.ecs.replication.Authority
import dev.chieppa.ease.ecs.replication.NetworkId
import dev.chieppa.ease.ecs.replication.predicts
import dev.chieppa.ease.ecs.replication.replication

/**
 * The entities a client's [world] neither holds nor predicts, drawn a little in the past: for each
 * replicated type with `@Interpolated` fields, the last [capacity] distinct states received, each
 * with its snapshot's tick. Between receives the world shows [present]'s blend of them; around each
 * receive, [beforeReceive] puts the newest state back so received deltas build on it, and
 * [snapshotApplied] keeps each new state. Types without interpolated fields show what was received
 * at once. Nothing here allocates per tick: an entity's copies are made when it is first seen.
 */
public class InterpolatedEntities(
  private val world: World,
  private val capacity: Int = DEFAULT_CAPACITY,
) {
  init {
    require(capacity >= 2) { "Interpolating needs at least 2 states kept, not $capacity" }
  }

  private val replication = world.replication
  private val interpolated =
    BooleanArray(replication.types.size) {
      replication.types[it].serializer.interpolatedFields != 0
    }
  private val held = world.family { all(Authority) }
  private val slots = SlotTable<InterpolatedEntity>()

  /** How many entities are drawn in the past, as of the last scan. */
  public val size: Int
    get() = slots.activeCount

  /** Puts each entity's newest received state back, before a receive applies deltas to it. */
  public fun beforeReceive() {
    scan()
    for (index in 0 until slots.activeCount) slots.activeAt(index).loadNewest(world)
  }

  /** Keeps each entity's state as the snapshot of [tick], just applied, left it. */
  public fun snapshotApplied(tick: Long) {
    scan()
    for (index in 0 until slots.activeCount) slots.activeAt(index).push(world, tick)
  }

  /**
   * Sets each entity's interpolated state to what it was at [time], in ticks. An entity removed
   * since the last scan, or whose id a new entity took, is left alone.
   */
  public fun present(time: Double) {
    for (index in 0 until slots.activeCount) {
      val slot = slots.activeAt(index)
      if (slot.entity in world && slot.carriesSame(world, slot.entity, interpolated)) {
        slot.present(world, time)
      }
    }
  }

  private fun scan() {
    slots.deactivateAll()
    held.forEach { entity ->
      if (!replication.hasAuthority(entity) && !replication.predicts(entity)) track(entity)
    }
  }

  private fun track(entity: Entity) {
    val id = replication.networkIdOf(entity)
    val slot = slots[entity.id]
    val same = slot != null && slot.entity == entity && slot.networkId == id
    if (!same || !checkNotNull(slot).carriesSame(world, entity, interpolated)) {
      if (!carriesInterpolated(entity)) return
      slots[entity.id] = newSlot(entity, id)
    }
    slots.activate(entity.id)
  }

  private fun carriesInterpolated(entity: Entity): Boolean {
    for (id in interpolated.indices) {
      if (interpolated[id] && replication.types[id].isOn(world, entity)) return true
    }
    return false
  }

  private fun newSlot(entity: Entity, id: NetworkId): InterpolatedEntity {
    val types = replication.types
    val carried = (0 until types.size).filter { interpolated[it] && types[it].isOn(world, entity) }
    return InterpolatedEntity(entity, id, carried.toIntArray(), replication, capacity)
  }

  public companion object {
    /** States kept per entity: enough for a quarter second of snapshots at 60 ticks a second. */
    public const val DEFAULT_CAPACITY: Int = 16
  }
}
