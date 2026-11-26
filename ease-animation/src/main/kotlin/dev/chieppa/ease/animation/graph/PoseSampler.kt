package dev.chieppa.ease.animation.graph

import dev.chieppa.ease.animation.pose.ChannelSampler
import dev.chieppa.ease.animation.pose.PoseBlend
import dev.chieppa.ease.scene.transform.WORLD_TRANSFORM_FLOATS

/**
 * Works out the pose a [Playback] of [graph] stands for: each playing state's clips sampled at the
 * state's normalized time over the rest pose (so joints a clip leaves alone keep their rest
 * transform), the two weighted clips of a blend blended, and the faded-from state blended into the
 * current one by the fade. Scratch poses are made once, so sampling allocates nothing.
 */
internal class PoseSampler(private val graph: AnimatorGraph) {
  private val joints = graph.joints.size
  private val clipPose = FloatArray(joints * WORLD_TRANSFORM_FLOATS)
  private val statePose = FloatArray(joints * WORLD_TRANSFORM_FLOATS)
  private val fromPose = FloatArray(joints * WORLD_TRANSFORM_FLOATS)

  /** Writes [playback]'s pose into [out], starting from [rest]; both hold one transform a joint. */
  fun sample(playback: Playback, rest: FloatArray, out: FloatArray) {
    val from = playback.fromState
    if (from < 0) {
      statePose(playback, playback.state, playback.phase, rest, out)
      return
    }
    statePose(playback, playback.state, playback.phase, rest, statePose)
    statePose(playback, from, playback.fromPhase, rest, fromPose)
    PoseBlend.blend(fromPose, statePose, playback.fade, out, joints)
  }

  private fun statePose(
    playback: Playback,
    state: Int,
    phase: Float,
    rest: FloatArray,
    dst: FloatArray,
  ) {
    val clips = graph.states[state].clips
    var first = -1
    var second = -1
    var secondWeight = 0f
    for (clip in clips.indices) {
      val weight = playback.weightInState(state, clip)
      if (weight <= 0f) continue
      if (first < 0) {
        first = clip
      } else {
        second = clip
        secondWeight = weight
      }
    }
    clip(clips[maxOf(first, 0)], phase, rest, dst)
    if (second >= 0) {
      clip(clips[second], phase, rest, clipPose)
      PoseBlend.blend(dst, clipPose, secondWeight, dst, joints)
    }
  }

  private fun clip(index: Int, phase: Float, rest: FloatArray, dst: FloatArray) {
    System.arraycopy(rest, 0, dst, 0, joints * WORLD_TRANSFORM_FLOATS)
    val clip = graph.clips[index]
    val time = phase * clip.asset.duration
    val channels = clip.asset.channels
    for (channel in 0 until channels.size) {
      ChannelSampler.sample(
        channels[channel],
        time,
        dst,
        clip.joints[channel] * WORLD_TRANSFORM_FLOATS,
      )
    }
  }
}
