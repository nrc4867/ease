package dev.chieppa.ease.ecs.prediction

import com.github.quillraven.fleks.Entity
import kotlin.math.sqrt

private const val INITIAL_ENTITIES = 64

/** An offset this small counts as none. */
private const val SETTLED = 1e-4f

/**
 * How far rendering draws each entity from where its state puts it, by entity id, so a corrected
 * prediction glides to its new place instead of jumping: a correction adds the distance it moved
 * the entity back the other way, and every tick each offset keeps [keep] of itself. An offset
 * longer than [snapDistance] is dropped, so a large correction snaps. Nothing here allocates once
 * its arrays have grown to the world's entity ids.
 */
public class CorrectionOffsets(
  private val keep: Float = DEFAULT_KEEP,
  private val snapDistance: Float = DEFAULT_SNAP_DISTANCE,
) {
  init {
    require(keep in 0f..<1f) { "An offset keeps from 0 up to but not all of itself, not $keep" }
    require(snapDistance >= 0f) { "A snap distance is 0 or more, not $snapDistance" }
  }

  private var offsetsX = FloatArray(INITIAL_ENTITIES)
  private var offsetsY = FloatArray(INITIAL_ENTITIES)
  private var offsetsZ = FloatArray(INITIAL_ENTITIES)

  /** Entity [id]'s offset along x. */
  public fun x(id: Int): Float = if (id < offsetsX.size) offsetsX[id] else 0f

  /** Entity [id]'s offset along y. */
  public fun y(id: Int): Float = if (id < offsetsY.size) offsetsY[id] else 0f

  /** Entity [id]'s offset along z. */
  public fun z(id: Int): Float = if (id < offsetsZ.size) offsetsZ[id] else 0f

  /**
   * Adds ([x], [y], [z]) to [entity]'s offset, dropping it when it grows past the snap distance.
   */
  public fun add(entity: Entity, x: Float, y: Float, z: Float) {
    val id = entity.id
    if (id >= offsetsX.size) grow(id)
    val newX = offsetsX[id] + x
    val newY = offsetsY[id] + y
    val newZ = offsetsZ[id] + z
    val snaps = sqrt(newX * newX + newY * newY + newZ * newZ) > snapDistance
    offsetsX[id] = if (snaps) 0f else newX
    offsetsY[id] = if (snaps) 0f else newY
    offsetsZ[id] = if (snaps) 0f else newZ
  }

  /** Drops [entity]'s offset. */
  public fun clear(entity: Entity) {
    val id = entity.id
    if (id >= offsetsX.size) return
    offsetsX[id] = 0f
    offsetsY[id] = 0f
    offsetsZ[id] = 0f
  }

  /** Shrinks every offset by one tick's worth. */
  public fun decay() {
    shrink(offsetsX)
    shrink(offsetsY)
    shrink(offsetsZ)
  }

  private fun shrink(offsets: FloatArray) {
    for (index in offsets.indices) {
      val kept = offsets[index] * keep
      offsets[index] = if (kept > -SETTLED && kept < SETTLED) 0f else kept
    }
  }

  private fun grow(id: Int) {
    val size = maxOf(id + 1, offsetsX.size * 2)
    offsetsX = offsetsX.copyOf(size)
    offsetsY = offsetsY.copyOf(size)
    offsetsZ = offsetsZ.copyOf(size)
  }

  public companion object {
    /** What an offset keeps of itself each tick: most of a correction is gone in 10 ticks. */
    public const val DEFAULT_KEEP: Float = 0.75f

    /** The longest correction smoothed, in world units; a longer one snaps. */
    public const val DEFAULT_SNAP_DISTANCE: Float = 2f
  }
}
