package dev.chieppa.ease.physics.jolt

import com.github.stephengold.joltjni.CustomCollidePointCollector

/**
 * Collects the body ids a point query finds, into an array it keeps, reading each from Jolt's
 * `CollidePointResult` in place (its body id is the first field) so collecting makes no objects.
 * Hits past [capacity] are dropped.
 */
internal class TriggerHits(capacity: Int) : CustomCollidePointCollector() {
  val bodies: IntArray = IntArray(capacity)
  var count: Int = 0
    private set

  /** Forgets the hits of the last query. */
  fun clear() {
    reset()
    count = 0
  }

  override fun addHit(hitVa: Long) {
    if (count < bodies.size) bodies[count++] = JoltMemory.getInt(hitVa, 0)
  }
}
