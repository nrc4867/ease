package dev.chieppa.ease.animation.pose

import dev.chieppa.ease.animation.assets.ChannelPath
import dev.chieppa.ease.animation.assets.ClipChannel
import dev.chieppa.ease.animation.assets.Interpolation
import dev.chieppa.ease.math.Quats

/** Where a pose's rotation starts within one joint's floats. */
internal const val POSE_ROTATION_AT: Int = 3

/** Where a pose's scale starts within one joint's floats. */
internal const val POSE_SCALE_AT: Int = 7

private const val CUBIC_PARTS = 3
private const val QUAT = 4

/**
 * Samples one clip channel at a time into a pose: the value at that time written over the
 * property's floats of the joint at a given offset. Times before the first key hold the first value
 * and after the last the last; [Interpolation.STEP] holds each key until the next,
 * [Interpolation.LINEAR] blends straight (rotations along the shortest arc) and
 * [Interpolation.CUBICSPLINE] follows glTF's cubic Hermite spline (rotations normalized after).
 * Every result depends on the inputs alone, through `StrictMath`, so every machine samples the same
 * bits; sampling allocates nothing.
 */
internal object ChannelSampler {
  /**
   * Writes [channel]'s value at [time] seconds into the joint whose floats start at [joint] in
   * [pose]. A morph weight channel writes nothing.
   */
  fun sample(channel: ClipChannel, time: Float, pose: FloatArray, joint: Int) {
    val size = channel.path.floats
    if (size == 0 || channel.times.isEmpty()) return
    val at = joint + offsetOf(channel.path)
    val times = channel.times
    val key = keyBefore(times, time)
    val cubic = channel.interpolation == Interpolation.CUBICSPLINE
    val stride = if (cubic) size * CUBIC_PARTS else size
    val valueAt = if (cubic) size else 0
    val last = times.size - 1
    when {
      key < 0 -> copy(channel.values, valueAt, pose, at, size)
      key >= last -> copy(channel.values, last * stride + valueAt, pose, at, size)
      channel.interpolation == Interpolation.STEP ->
        copy(channel.values, key * stride, pose, at, size)
      else -> between(channel, key, time, pose, at)
    }
  }

  /** Where [path]'s floats start within one joint's. */
  private fun offsetOf(path: ChannelPath): Int =
    when (path) {
      ChannelPath.ROTATION -> POSE_ROTATION_AT
      ChannelPath.SCALE -> POSE_SCALE_AT
      else -> 0
    }

  /** Writes [channel]'s value at [time], between key [key] and the next, into [pose] at [at]. */
  private fun between(channel: ClipChannel, key: Int, time: Float, pose: FloatArray, at: Int) {
    val times = channel.times
    val size = channel.path.floats
    val span = times[key + 1] - times[key]
    val t = if (span > 0f) (time - times[key]) / span else 0f
    if (channel.interpolation == Interpolation.CUBICSPLINE) {
      hermite(channel.values, key, size, span, t, pose, at)
      if (channel.path == ChannelPath.ROTATION) Quats.normalize(pose, at, pose, at)
    } else {
      linear(channel.values, key, size, t, channel.path, pose, at)
    }
  }

  /** The last key at or before [time], -1 when [time] is before the first. */
  private fun keyBefore(times: FloatArray, time: Float): Int {
    var low = 0
    var high = times.size - 1
    if (time < times[0]) return -1
    while (low < high) {
      val middle = (low + high + 1) ushr 1
      if (times[middle] <= time) low = middle else high = middle - 1
    }
    return low
  }

  @Suppress("LongParameterList") // Arrays and offsets, so sampling allocates nothing.
  private fun linear(
    values: FloatArray,
    key: Int,
    size: Int,
    t: Float,
    path: ChannelPath,
    pose: FloatArray,
    at: Int,
  ) {
    val from = key * size
    if (path == ChannelPath.ROTATION) {
      Quats.slerp(values, from, values, from + QUAT, t, pose, at)
      return
    }
    for (index in 0 until size) {
      val a = values[from + index]
      pose[at + index] = a + (values[from + size + index] - a) * t
    }
  }

  /**
   * glTF's cubic spline between key [key] and the next: each key holds an in-tangent, a value and
   * an out-tangent, the tangents scaled by the keys' [span] in seconds.
   */
  @Suppress("LongParameterList") // Arrays and offsets, so sampling allocates nothing.
  private fun hermite(
    values: FloatArray,
    key: Int,
    size: Int,
    span: Float,
    t: Float,
    pose: FloatArray,
    at: Int,
  ) {
    val stride = size * CUBIC_PARTS
    val start = key * stride
    val next = start + stride
    val t2 = t * t
    val t3 = t2 * t
    val h00 = 2f * t3 - 3f * t2 + 1f
    val h10 = t3 - 2f * t2 + t
    val h01 = -2f * t3 + 3f * t2
    val h11 = t3 - t2
    for (index in 0 until size) {
      val value = values[start + size + index]
      val out = values[start + 2 * size + index]
      val nextIn = values[next + index]
      val nextValue = values[next + size + index]
      pose[at + index] = h00 * value + h10 * span * out + h01 * nextValue + h11 * span * nextIn
    }
  }

  private fun copy(values: FloatArray, from: Int, pose: FloatArray, at: Int, size: Int) {
    System.arraycopy(values, from, pose, at, size)
  }
}
