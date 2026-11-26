package dev.chieppa.ease.animation.runtime

import dev.chieppa.ease.animation.events.AnimationEvents
import dev.chieppa.ease.animation.probe.ProbeRunner
import dev.chieppa.ease.scene.transform.WorldTransforms

/**
 * Reports the clip events an animator's playback passed in its last advance, to [events], and runs
 * the probes listening for each through [probes]. Events come from the heaviest clip of the state
 * with the larger share of a crossfade, so a blend or a fade reports each footstep once; an event
 * fires when playback passes its time, wrapping round with a looping state. Reporting allocates
 * nothing.
 */
internal class EventDispatch(private val events: AnimationEvents, private val probes: ProbeRunner) {
  /** Reports [instance]'s events, its probes placed through [poses] and [worlds]. */
  fun fire(instance: AnimatorInstance, poses: AnimatedPoses, worlds: WorldTransforms?) {
    val playback = instance.playback
    val state = playback.eventState
    val graphState = instance.graph.states[state]
    val clip = instance.graph.clips[graphState.clips[playback.heaviestClip(state)]]
    val from = playback.eventPrevious
    val to = playback.eventPhase
    val wrapped = to < from
    val phases = clip.eventPhases
    for (index in phases.indices) {
      val at = phases[index]
      val passed = if (wrapped) at > from || at <= to else at > from && at <= to
      if (passed) {
        val name = clip.asset.events[index].name
        events.clipEvent(instance.entity, name)
        probes.run(instance, name, poses, worlds)
      }
    }
  }
}
