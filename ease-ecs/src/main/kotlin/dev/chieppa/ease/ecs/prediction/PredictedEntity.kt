package dev.chieppa.ease.ecs.prediction

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.ecs.replication.NetworkId
import dev.chieppa.ease.ecs.replication.Replication

private const val NO_TICK = Long.MIN_VALUE

/**
 * What a client keeps of one entity it predicts, [entity] under [networkId], for each replicated
 * type it carries ([typeIds]): its state as last predicted ([present]), as the authority last sent
 * it ([authoritative]), and as predicted for each of the last [historyTicks] ticks.
 */
internal class PredictedEntity(
  val entity: Entity,
  val networkId: NetworkId,
  val typeIds: IntArray,
  private val replication: Replication,
  historyTicks: Int,
) {
  private val types = replication.types
  val present: Array<Any> = copies()
  val authoritative: Array<Any> = copies()
  private val history = Array(historyTicks) { copies() }
  private val ticks = LongArray(historyTicks) { NO_TICK }

  /** The tick of the newest snapshot that carried the entity, or -1 before any. */
  var authoritativeTick: Long = -1L

  /** Whether a snapshot applied in the receive under way carried the entity. */
  var carriedNow: Boolean = false

  /** The number of the last scan that found the entity predicted. */
  var lastSeen: Long = 0L

  /** True until the entity's first replay: it has no prediction to compare yet. */
  var fresh: Boolean = true
    private set

  /** Whether the last replay may smooth the entity: it had been predicted before it. */
  var smooths: Boolean = false
    private set

  /** Marks the entity as replayed: from now on its corrections are smoothed. */
  fun settle() {
    smooths = !fresh
    fresh = false
  }

  /** Whether [entity] carries exactly the replicated types [typeIds] names. */
  fun carriesSame(world: World, entity: Entity): Boolean {
    var count = 0
    for (id in 0 until types.size) {
      if (types[id].isOn(world, entity)) {
        if (count >= typeIds.size || typeIds[count] != id) return false
        count++
      }
    }
    return count == typeIds.size
  }

  /** Copies the entity's components into [into]. */
  fun save(world: World, into: Array<Any>) {
    for (index in typeIds.indices) types[typeIds[index]].copies.save(world, entity, into[index])
  }

  /** Copies [from] into the entity's components, as the replication layer's own change. */
  fun load(world: World, from: Array<Any>) {
    for (index in typeIds.indices) {
      val typeId = typeIds[index]
      types[typeId].copies.load(world, entity, from[index])
      replication.received(entity, typeId)
    }
  }

  /** Keeps the entity's components as the prediction for [tick]. */
  fun record(world: World, tick: Long) {
    val slot = slotOf(tick)
    save(world, history[slot])
    ticks[slot] = tick
  }

  /** Whether the entity's components go on the wire as the prediction kept for [tick] would. */
  fun matches(world: World, tick: Long): Boolean {
    val slot = slotOf(tick)
    var same = tick >= 0 && ticks[slot] == tick
    for (index in typeIds.indices) {
      if (same && types[typeIds[index]].copies.wireDiffers(world, entity, history[slot][index])) {
        same = false
      }
    }
    return same
  }

  /** Puts the prediction kept for [tick] into the world and returns true, or returns false. */
  fun loadPrediction(world: World, tick: Long): Boolean {
    val slot = slotOf(tick)
    if (tick < 0 || ticks[slot] != tick) return false
    load(world, history[slot])
    return true
  }

  /** The index of [typeId] among [typeIds], or -1. */
  fun indexOf(typeId: Int): Int = typeIds.indexOf(typeId)

  private fun slotOf(tick: Long): Int = Math.floorMod(tick, ticks.size.toLong()).toInt()

  private fun copies(): Array<Any> = Array(typeIds.size) { types[typeIds[it]].copies.create() }
}
