package dev.chieppa.ease.ecs.prediction

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.replication.Authority
import dev.chieppa.ease.ecs.replication.NetworkId
import dev.chieppa.ease.ecs.replication.predicts
import dev.chieppa.ease.ecs.replication.replication

/**
 * A client's own entities, the ones its [world] predicts (see `Replication.predicts`), with what
 * reconciling them needs: each one's replicated state as the authority last sent it, as last
 * predicted, and as predicted for each of the last [historyTicks] ticks.
 *
 * Around each receive: [beforeReceive] puts the authority's last state back, so received deltas
 * build on it; [afterReceive] keeps the new authority state and compares it with the prediction for
 * the snapshot's tick. When they match, the prediction comes back; when they differ, the world
 * keeps the authority's state, and the caller replays its input from the tick returned, [record]ing
 * each tick again. Only predicted entities are saved, restored or compared; the rest of the world
 * is never rewound. Nothing here allocates per tick: an entity's copies are made when it is first
 * predicted.
 */
public class PredictedEntities(
  private val world: World,
  private val historyTicks: Int = DEFAULT_HISTORY_TICKS,
) {
  init {
    require(historyTicks > 0) { "A prediction history holds at least 1 tick, not $historyTicks" }
  }

  private val replication = world.replication
  private val transformType = replication.types.idOf(LocalTransform)
  private val held = world.family { all(Authority) }
  private val slots = SlotTable<PredictedEntity>()

  /** How many entities the world predicts, as of the last receive. */
  public val size: Int
    get() = slots.activeCount

  /** Saves each predicted entity's prediction, then puts back the authority's last state. */
  public fun beforeReceive() {
    scan()
    for (index in 0 until slots.activeCount) {
      val slot = slots.activeAt(index)
      slot.save(world, slot.present)
      slot.load(world, slot.authoritative)
    }
  }

  /**
   * Keeps what the receive left as the authority's state and returns the tick to replay input from,
   * or [NO_REPLAY] when the prediction stands and is back in the world. A replay is due when
   * [newSnapshot] carried a state for [snapshotTick] that differs on the wire from the prediction
   * for that tick, or when an entity began to be predicted; the world then holds the authority's
   * state as of [snapshotTick].
   */
  public fun afterReceive(snapshotTick: Long, newSnapshot: Boolean): Long {
    scan()
    var replay = false
    for (index in 0 until slots.activeCount) {
      val slot = slots.activeAt(index)
      slot.save(world, slot.authoritative)
      if (slot.fresh || (newSnapshot && !slot.matches(world, snapshotTick))) replay = true
    }
    if (replay && snapshotTick >= 0) {
      for (index in 0 until slots.activeCount) slots.activeAt(index).settle()
      return snapshotTick
    }
    for (index in 0 until slots.activeCount) {
      val slot = slots.activeAt(index)
      if (!slot.fresh) slot.load(world, slot.present)
    }
    return NO_REPLAY
  }

  /** Keeps each predicted entity's state as the prediction for [tick]. */
  public fun record(tick: Long) {
    for (index in 0 until slots.activeCount) slots.activeAt(index).record(world, tick)
  }

  /** Forgets every prediction kept, as when input to replay them is gone. */
  public fun forget() {
    for (index in 0 until slots.activeCount) slots.activeAt(index).forget()
  }

  /**
   * Adds to [offsets] how far the last replay moved each predicted entity's transform from where
   * the prediction before it had put it, so drawing it glides rather than jumps. Entities predicted
   * for the first time are left to appear where they are.
   */
  public fun smooth(offsets: CorrectionOffsets) {
    if (transformType < 0) return
    for (index in 0 until slots.activeCount) {
      val slot = slots.activeAt(index)
      val at = slot.indexOf(transformType)
      if (!slot.smooths || at < 0) continue
      val before = slot.present[at] as LocalTransform
      val now = with(world) { slot.entity[LocalTransform] }
      offsets.add(slot.entity, before.x - now.x, before.y - now.y, before.z - now.z)
    }
  }

  private fun scan() {
    slots.deactivateAll()
    held.forEach { entity -> if (replication.predicts(entity)) track(entity) }
  }

  private fun track(entity: Entity) {
    val id = replication.networkIdOf(entity)
    val slot = slots[entity.id]
    val same = slot != null && slot.entity == entity && slot.networkId == id
    if (!same || !checkNotNull(slot).carriesSame(world, entity)) {
      slots[entity.id] = newSlot(entity, id)
    }
    slots.activate(entity.id)
  }

  private fun newSlot(entity: Entity, id: NetworkId): PredictedEntity {
    val types = replication.types
    val carried = (0 until types.size).filter { types[it].isOn(world, entity) }.toIntArray()
    val slot = PredictedEntity(entity, id, carried, replication, historyTicks)
    slot.save(world, slot.present)
    slot.save(world, slot.authoritative)
    return slot
  }

  public companion object {
    /** What [afterReceive] returns when no replay is due. */
    public const val NO_REPLAY: Long = -1L

    /** Ticks of prediction kept: two seconds at 60 ticks a second. */
    public const val DEFAULT_HISTORY_TICKS: Int = 128
  }
}
