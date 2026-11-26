package dev.chieppa.ease.animation

import dev.chieppa.ease.animation.assets.ChannelPath
import dev.chieppa.ease.animation.assets.ClipChannel
import dev.chieppa.ease.animation.assets.Interpolation
import dev.chieppa.ease.animation.pose.ChannelSampler
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals

/** Channels sample as glTF says: step holds, linear blends, cubic splines follow their tangents. */
class ChannelSamplingTest {
  private val pose = FloatArray(10)

  private fun x(channel: ClipChannel, time: Float): Float {
    ChannelSampler.sample(channel, time, pose, 0)
    return pose[0]
  }

  private fun translation(interpolation: Interpolation, values: FloatArray) =
    ClipChannel("0000000000000001", ChannelPath.TRANSLATION, interpolation, TIMES, values)

  @Test
  fun `a step channel holds each key until the next`() {
    val channel = translation(Interpolation.STEP, floatArrayOf(1f, 0f, 0f, 3f, 0f, 0f))

    assertEquals(1f, x(channel, 0.99f))
    assertEquals(3f, x(channel, 1f))
    assertEquals(3f, x(channel, 5f))
  }

  @Test
  fun `a linear channel blends straight and holds its ends`() {
    val channel = translation(Interpolation.LINEAR, floatArrayOf(1f, 0f, 0f, 3f, 0f, 0f))

    assertEquals(1f, x(channel, -1f))
    assertEquals(2.5f, x(channel, 0.75f))
    assertEquals(3f, x(channel, 2f))
  }

  @Test
  fun `a cubic spline channel follows its keys' tangents`() {
    // From 0, leaving with tangent 3, to 1, arriving flat, over one second: the Hermite curve
    // 3(t^3 - 2t^2 + t) + (3t^2 - 2t^3), which is 0.875 halfway.
    val values =
      floatArrayOf(
        0f,
        0f,
        0f,
        0f,
        0f,
        0f,
        3f,
        0f,
        0f, // key 0: in, value, out
        0f,
        0f,
        0f,
        1f,
        0f,
        0f,
        0f,
        0f,
        0f, // key 1: in, value, out
      )
    val channel = translation(Interpolation.CUBICSPLINE, values)

    assertEquals(0.875f, x(channel, 0.5f), 1e-6f)
    assertEquals(1f, x(channel, 1f))
  }

  @Test
  fun `a linear rotation turns along the shortest arc`() {
    val half = 0.5f
    val channel =
      ClipChannel(
        "0000000000000001",
        ChannelPath.ROTATION,
        Interpolation.LINEAR,
        TIMES,
        floatArrayOf(0f, 0f, 0f, 1f, sin(half), 0f, 0f, cos(half)),
      )

    ChannelSampler.sample(channel, 0.5f, pose, 0)

    assertEquals(sin(half / 2f), pose[3], 1e-6f)
    assertEquals(cos(half / 2f), pose[6], 1e-6f)
  }

  private companion object {
    val TIMES = floatArrayOf(0f, 1f)
  }
}
