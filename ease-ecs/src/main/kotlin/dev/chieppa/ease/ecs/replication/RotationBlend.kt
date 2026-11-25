package dev.chieppa.ease.ecs.replication

import kotlin.math.sqrt

/**
 * Blends unit quaternions for interpolated rotation groups: toward the nearer of the target and its
 * negation, then renormalized; when the blend has no length, the start is kept. Callers pass the
 * four components of each end and take one component of the result at a time, so nothing here
 * allocates or keeps state.
 */
public object RotationBlend {
  /**
   * Component [index] (0 to 3, in the group's field order) of the blend [amount] of the way from
   * ([fromX], [fromY], [fromZ], [fromW]) to [to], whose four components are its 0th to 3rd.
   */
  @Suppress("LongParameterList") // Four components a side, kept apart so nothing allocates.
  public fun component(
    index: Int,
    fromX: Float,
    fromY: Float,
    fromZ: Float,
    fromW: Float,
    toX: Float,
    toY: Float,
    toZ: Float,
    toW: Float,
    amount: Float,
  ): Float {
    val dot = fromX * toX + fromY * toY + fromZ * toZ + fromW * toW
    val sign = if (dot < 0f) -1f else 1f
    val x = fromX + (toX * sign - fromX) * amount
    val y = fromY + (toY * sign - fromY) * amount
    val z = fromZ + (toZ * sign - fromZ) * amount
    val w = fromW + (toW * sign - fromW) * amount
    val length = sqrt(x * x + y * y + z * z + w * w)
    val blended = pick(index, x, y, z, w)
    return if (length > 0f) blended / length else pick(index, fromX, fromY, fromZ, fromW)
  }

  private fun pick(index: Int, x: Float, y: Float, z: Float, w: Float): Float =
    when (index) {
      0 -> x
      1 -> y
      2 -> z
      else -> w
    }

  /** How many fields a rotation group has. */
  public const val COMPONENTS: Int = 4
}
