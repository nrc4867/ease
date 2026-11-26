package dev.chieppa.ease.animation.runtime

import dev.chieppa.ease.animation.events.AnimationEvents
import dev.chieppa.ease.animation.probe.ProbeRunner
import dev.chieppa.ease.scene.transform.WorldTransforms

/**
 * Reports the clip events an animator's playback passed in its last advance, to [events], and runs
 * the probes listening for each through [probes]. Events come from the heaviest clip of the state
 * with the larger share of a crossfade, so a blend or a fade reports each footstep once. An event
 * fires each time playback passes its time: once per loop the advance went round, so an advance of
 * a whole cycle or more fires every event once per cycle. Reporting allocates nothing.
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
    val wraps = playback.eventWraps
    val phases = clip.eventPhases
    for (index in phases.indices) {
      val at = phases[index]
      // Times k + at passed in (from, wraps + to], for whole k.
      val passed = wraps + (if (to >= at) 1 else 0) - (if (from >= at) 1 else 0)
      val name = clip.asset.events[index].name
      repeat(passed) {
        events.clipEvent(instance.entity, name)
        probes.run(instance, name, poses, worlds)
      }
    }
  }
}
