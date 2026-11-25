package dev.chieppa.ease.physics.jolt

import com.github.stephengold.joltjni.RRayCast
import com.github.stephengold.joltjni.RayCastResult

/**
 * One reusable Jolt ray and result. jolt-jni's `RRayCast` is immutable, so [aim] rewrites its
 * origin and direction in place, in Jolt's layout (two 16-byte vectors, the fourth lane a copy of
 * the third), and resets the result's fraction to the full ray; nothing is made per cast.
 */
internal class RayScratch : AutoCloseable {
  val ray: RRayCast = RRayCast(0.0, 0.0, 0.0, 0f, -1f, 0f)
  val result: RayCastResult = RayCastResult()

  /** Aims the ray from the origin along the vector, whose length is the ray's reach. */
  @Suppress("LongParameterList") // A ray is six numbers; boxing them would allocate.
  fun aim(originX: Float, originY: Float, originZ: Float, rayX: Float, rayY: Float, rayZ: Float) {
    val address = ray.va()
    putVector(address, ORIGIN, originX, originY, originZ)
    putVector(address, DIRECTION, rayX, rayY, rayZ)
    JoltMemory.putFloat(result.va(), FRACTION, FULL_RAY)
  }

  private fun putVector(address: Long, at: Long, x: Float, y: Float, z: Float) {
    JoltMemory.putFloat(address, at, x)
    JoltMemory.putFloat(address, at + LANE, y)
    JoltMemory.putFloat(address, at + LANE + LANE, z)
    JoltMemory.putFloat(address, at + LANE + LANE + LANE, z)
  }

  override fun close() {
    result.close()
    ray.close()
  }

  private companion object {
    const val LANE = 4L
    const val ORIGIN = 0L
    const val DIRECTION = 16L

    /** `RayCastResult.mFraction`, after the 4-byte body id. */
    const val FRACTION = 4L

    /** Jolt's initial fraction: anything hit along the whole ray is closer. */
    const val FULL_RAY = 1.0f + 1.0e-4f
  }
}
