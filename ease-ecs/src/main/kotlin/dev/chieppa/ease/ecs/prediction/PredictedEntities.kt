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
 * reconciling them needs: each one's replicated state as the authority last sent it and the tick of
 * the snapshot that carried it, its state as last predicted, and its prediction for each of the
 * last [historyTicks] ticks. Corrections are drawn smoothly through [offsets].
 *
 * Around each receive: [beforeReceive] puts the authority's last state back, so received deltas
 * build on it; [snapshotApplied] notes which entities each snapshot carried; [afterReceive] keeps
 * the new authority state and compares each entity a snapshot carried with its prediction for that
 * snapshot's tick. When they all match, the prediction comes back; otherwise the world holds every
 * entity as of the earliest disagreeing tick (the authority's state where it disagreed, the
 * prediction elsewhere) and the caller replays its input from there, [record]ing each tick again.
 * Only predicted entities are saved, restored or compared; the rest of the world is never rewound.
 * Nothing here allocates per tick: an entity's copies are made when it is first predicted.
 */
public class PredictedEntities(
  private val world: World,
  private val offsets: CorrectionOffsets = CorrectionOffsets(),
  private val historyTicks: Int = DEFAULT_HISTORY_TICKS,
) {
  init {
    require(historyTicks > 0) { "A prediction history holds at least 1 tick, not $historyTicks" }
  }

  private val replication = world.replication
  private val transformType = replication.types.idOf(LocalTransform)
  private val held = world.family { all(Authority) }
  private val slots = SlotTable<PredictedEntity>()
  private var previous = IntArray(INITIAL_PREVIOUS)
  private var scans = 0L

  /** How many entities the world predicts, as of the last scan. */
  public val size: Int
    get() = slots.activeCount

  /** Saves each predicted entity's prediction, then puts back the authority's last state. */
  public fun beforeReceive() {
    scan()
    forEachActive { slot ->
      slot.save(world, slot.present)
      slot.load(world, slot.authoritative)
      slot.carriedNow = false
    }
  }

  /** Notes which predicted entities the snapshot of [tick], just applied, carried. */
  public fun snapshotApplied(tick: Long) {
    scan()
    forEachActive { slot ->
      if (replication.carried.inLastPacket(slot.entity)) {
        slot.authoritativeTick = tick
        slot.carriedNow = true
      }
    }
  }

  /**
   * Keeps what the receive left as the authority's state and returns the tick to replay input from,
   * or [NO_REPLAY] when every prediction stands and is back in the world. A replay is due when an
   * entity a snapshot carried differs on the wire from its prediction for that snapshot's tick, or
   * when an entity began to be predicted, which replays from [latestSnapshotTick].
   */
  public fun afterReceive(latestSnapshotTick: Long): Long {
    scan()
    var from = Long.MAX_VALUE
    forEachActive { slot ->
      slot.save(world, slot.authoritative)
      val wrong = slot.carriedNow && !slot.matches(world, slot.authoritativeTick)
      if (wrong) from = minOf(from, slot.authoritativeTick)
      if (slot.fresh && latestSnapshotTick >= 0) from = minOf(from, latestSnapshotTick)
    }
    if (from == Long.MAX_VALUE) {
      forEachActive { slot -> if (!slot.fresh) slot.load(world, slot.present) }
      return NO_REPLAY
    }
    forEachActive { slot -> rewind(slot, from) }
    return from
  }

  /** Keeps each predicted entity's state as the prediction for [tick]. */
  public fun record(tick: Long) {
    forEachActive { slot -> slot.record(world, tick) }
  }

  /** Puts each predicted entity's prediction from before the receive back into the world. */
  public fun keepPredictions() {
    forEachActive { slot -> slot.load(world, slot.present) }
  }

  /**
   * Adds to the offsets how far the last replay moved each predicted entity's transform from where
   * the prediction before it had put it, so drawing it glides rather than jumps. Entities predicted
   * for the first time are left to appear where they are.
   */
  public fun smooth() {
    if (transformType < 0) return
    forEachActive { slot ->
      val at = slot.indexOf(transformType)
      if (slot.smooths && at >= 0) {
        val before = slot.present[at] as LocalTransform
        val now = with(world) { slot.entity[LocalTransform] }
        offsets.add(slot.entity, before.x - now.x, before.y - now.y, before.z - now.z)
      }
    }
  }

  /**
   * Holds [slot] as of [from]: the authority's state where that is its tick, else the prediction.
   */
  private fun rewind(slot: PredictedEntity, from: Long) {
    val authoritative = slot.fresh || (slot.carriedNow && slot.authoritativeTick == from)
    if (!authoritative && !slot.loadPrediction(world, from)) slot.load(world, slot.authoritative)
    slot.settle()
  }

  private inline fun forEachActive(action: (PredictedEntity) -> Unit) {
    for (index in 0 until slots.activeCount) action(slots.activeAt(index))
  }

  /**
   * Finds the predicted entities, and drops the offset of each entity the previous scan found that
   * this one does not.
   */
  private fun scan() {
    val before = slots.activeCount
    if (previous.size < before) previous = IntArray(maxOf(before, previous.size * 2))
    for (index in 0 until before) previous[index] = slots.activeAt(index).entity.id
    scans++
    slots.deactivateAll()
    held.forEach { entity -> if (replication.predicts(entity)) track(entity) }
    for (index in 0 until before) {
      val slot = slots[previous[index]] ?: continue
      if (slot.lastSeen != scans) offsets.clear(slot.entity)
    }
  }

  private fun track(entity: Entity) {
    val id = replication.networkIdOf(entity)
    var slot = slots[entity.id]
    val same = slot != null && slot.entity == entity && slot.networkId == id
    if (!same || !checkNotNull(slot).carriesSame(world, entity)) {
      offsets.clear(entity)
      slot = newSlot(entity, id)
      slots[entity.id] = slot
    }
    slot.lastSeen = scans
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

    private const val INITIAL_PREVIOUS = 16
  }
}
