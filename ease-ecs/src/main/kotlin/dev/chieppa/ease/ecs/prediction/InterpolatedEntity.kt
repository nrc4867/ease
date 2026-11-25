package dev.chieppa.ease.ecs.prediction

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.ecs.replication.NetworkId
import dev.chieppa.ease.ecs.replication.Replication

/**
 * The received states a client keeps of one entity it neither holds nor predicts, [entity] under
 * [networkId]: for each replicated type it carries with interpolated fields ([typeIds]), the last
 * [capacity] distinct states received, each with the tick of the snapshot that brought it.
 */
internal class InterpolatedEntity(
  val entity: Entity,
  val networkId: NetworkId,
  val typeIds: IntArray,
  private val replication: Replication,
  private val capacity: Int,
) {
  private val types = replication.types
  private val entries =
    Array(capacity) { Array(typeIds.size) { index -> types[typeIds[index]].copies.create() } }
  private val ticks = LongArray(capacity)
  private var newest = 0
  private var count = 0

  /** Whether [entity] carries exactly the interpolated types [typeIds] names. */
  fun carriesSame(world: World, entity: Entity, interpolated: BooleanArray): Boolean {
    var found = 0
    for (id in 0 until types.size) {
      if (!interpolated[id] || !types[id].isOn(world, entity)) continue
      if (found >= typeIds.size || typeIds[found] != id) return false
      found++
    }
    return found == typeIds.size
  }

  /** Keeps the entity's state as received at [tick], unless it is the newest one kept. */
  fun push(world: World, tick: Long) {
    if (count > 0 && !differsFromNewest(world)) return
    newest = (newest + 1) % capacity
    for (index in typeIds.indices) types[typeIds[index]]
      .copies
      .save(world, entity, entries[newest][index])
    ticks[newest] = tick
    count = minOf(count + 1, capacity)
  }

  /** Puts the newest state received back into the world, for the next deltas to build on. */
  fun loadNewest(world: World) {
    if (count > 0) blend(world, newest, newest, 0f)
  }

  /**
   * Sets the entity's state to what it was at [time], in ticks: blended between the two states
   * received around it, or held at the newest (or oldest) when [time] lies past them.
   */
  fun present(world: World, time: Double) {
    if (count == 0) return
    var back = 0
    while (back < count - 1 && ticks[slotBack(back)] > time) back++
    val before = slotBack(back)
    if (back == 0 || ticks[before] > time) {
      blend(world, before, before, 0f)
      return
    }
    val after = slotBack(back - 1)
    val amount = (time - ticks[before]) / (ticks[after] - ticks[before])
    blend(world, before, after, amount.toFloat())
  }

  private fun differsFromNewest(world: World): Boolean {
    for (index in typeIds.indices) {
      if (types[typeIds[index]].copies.differs(world, entity, entries[newest][index])) return true
    }
    return false
  }

  private fun blend(world: World, from: Int, to: Int, amount: Float) {
    for (index in typeIds.indices) {
      val typeId = typeIds[index]
      types[typeId].copies.blend(world, entity, entries[from][index], entries[to][index], amount)
      replication.received(entity, typeId)
    }
  }

  private fun slotBack(back: Int): Int = (newest - back + capacity) % capacity
}
