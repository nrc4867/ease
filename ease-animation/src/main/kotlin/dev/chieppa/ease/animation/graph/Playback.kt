package dev.chieppa.ease.animation.graph

import kotlin.math.floor

private const val NONE = -1
private const val HALF = 0.5f

/**
 * Where one animator is in its [graph]: the current state and how far through it, the state it is
 * crossfading from, the [parameters], and each clip's share of the pose ([clipWeights]). Each
 * [advance] takes the first transition whose conditions hold (only while no crossfade runs), moves
 * every playing state on by its own duration, and works out the weights. A state's clips share one
 * normalized time from 0 to 1, so blended clips of different lengths stay in step; a blend's
 * duration is its clips' durations weighted. Everything depends on the parameters and the steps
 * alone, so two peers given the same ones agree to the bit; advancing allocates nothing.
 */
internal class Playback(val graph: AnimatorGraph) {
  /** Each parameter's value, by index. */
  val parameters: FloatArray =
    FloatArray(graph.parameterNames.size) {
      graph.asset.parameters[it].initial
    }

  /** The state playing, or being faded to. */
  var state: Int = graph.start
    private set

  /** How far through [state], 0 to 1, and where it was before the last advance. */
  var phase: Float = 0f
    private set

  var previousPhase: Float = 0f
    private set

  /** The state being faded from, or -1. */
  var fromState: Int = NONE
    private set

  var fromPhase: Float = 0f
    private set

  private var fromPrevious = 0f
  private var fadeElapsed = 0f
  private var fadeDuration = 0f

  /** Each graph clip's share of the pose after the last advance; they sum to 1. */
  val clipWeights: FloatArray = FloatArray(graph.clips.size)

  /** How far the crossfade has come, 0 to 1; 1 when none runs. */
  val fade: Float
    get() = if (fromState == NONE) 1f else fadeElapsed / fadeDuration

  /** Moves playback on by [seconds]. */
  fun advance(seconds: Float) {
    if (fromState == NONE) takeTransition()
    previousPhase = phase
    phase = step(state, phase, seconds)
    if (fromState != NONE) {
      fromPrevious = fromPhase
      fromPhase = step(fromState, fromPhase, seconds)
      fadeElapsed += seconds
      if (fadeElapsed >= fadeDuration) fromState = NONE
    }
    weigh()
  }

  /** The state whose events fire this tick: the target, unless the fade is not yet half done. */
  val eventState: Int
    get() = if (fromState != NONE && fade < HALF) fromState else state

  /** Where [eventState] was before the last advance and is now. */
  val eventPrevious: Float
    get() = if (eventState == state) previousPhase else fromPrevious

  val eventPhase: Float
    get() = if (eventState == state) phase else fromPhase

  /**
   * The weight of clip [clip] (an index into the state's own clips) within state [state], from the
   * blend parameter; 1 for a single clip.
   */
  fun weightInState(state: Int, clip: Int): Float {
    val graphState = graph.states[state]
    val points = graphState.points
    if (graphState.parameter < 0 || points.size == 1) return if (clip == 0) 1f else 0f
    val value = parameters[graphState.parameter]
    val last = points.size - 1
    return when {
      value <= points[0] -> if (clip == 0) 1f else 0f
      value >= points[last] -> if (clip == last) 1f else 0f
      else -> {
        var upper = 1
        while (points[upper] < value) upper++
        val t = (value - points[upper - 1]) / (points[upper] - points[upper - 1])
        when (clip) {
          upper -> t
          upper - 1 -> 1f - t
          else -> 0f
        }
      }
    }
  }

  /** The index into the state's clips of state [state]'s heaviest clip. */
  fun heaviestClip(state: Int): Int {
    var best = 0
    var bestWeight = -1f
    for (clip in graph.states[state].clips.indices) {
      val weight = weightInState(state, clip)
      if (weight > bestWeight) {
        best = clip
        bestWeight = weight
      }
    }
    return best
  }

  private fun takeTransition() {
    val transitions = graph.transitions
    for (index in transitions.indices) {
      val transition = transitions[index]
      val leaves = if (transition.from == NONE) transition.to != state else transition.from == state
      if (leaves && holds(transition)) {
        enter(transition.to, transition.duration)
        return
      }
    }
  }

  private fun enter(target: Int, duration: Float) {
    if (duration > 0f) {
      fromState = state
      fromPhase = phase
      fromPrevious = phase
      fadeElapsed = 0f
      fadeDuration = duration
    }
    state = target
    phase = 0f
    previousPhase = 0f
  }

  private fun holds(transition: GraphTransition): Boolean {
    for (index in transition.parameters.indices) {
      val value = parameters[transition.parameters[index]]
      val bound = transition.values[index]
      val held = if (transition.above[index]) value > bound else value < bound
      if (!held) return false
    }
    return true
  }

  /** [phase] of [state] moved on by [seconds], wrapped when it loops and held at 1 when not. */
  private fun step(state: Int, phase: Float, seconds: Float): Float {
    val graphState = graph.states[state]
    val duration = duration(state)
    if (duration <= 0f) return 0f
    val moved = phase + seconds * graphState.speed / duration
    return if (graphState.loop) moved - floor(moved) else minOf(moved, 1f)
  }

  /** State [state]'s duration in seconds: its clips' durations, weighted. */
  private fun duration(state: Int): Float {
    val clips = graph.states[state].clips
    var total = 0f
    for (clip in clips.indices) {
      total += weightInState(state, clip) * graph.clips[clips[clip]].asset.duration
    }
    return total
  }

  private fun weigh() {
    clipWeights.fill(0f)
    val fade = fade
    addState(state, fade)
    if (fromState != NONE) addState(fromState, 1f - fade)
  }

  private fun addState(state: Int, weight: Float) {
    val clips = graph.states[state].clips
    for (clip in clips.indices) {
      clipWeights[clips[clip]] += weight * weightInState(state, clip)
    }
  }
}
