package dev.chieppa.ease.runtime.loop

import dev.chieppa.ease.platform.time.Clock

/**
 * Runs a [Simulation] at a fixed step from a [Clock]. Each [frame] runs the whole steps the time
 * since the last frame holds, up to [FixedStep.maxStepsPerFrame], and sets [alpha] for rendering
 * between the last two steps. Nothing here allocates per frame.
 */
public class FixedStepLoop(
  private val clock: Clock,
  private val step: FixedStep,
  private val simulation: Simulation,
) {
  private var started = false
  private var lastFrameNanos = 0L
  private var pendingNanos = 0L

  /** The tick the next step gets, which is also the number of steps run so far. */
  public var tick: Long = 0
    private set

  /**
   * How far the clock is between the last step and the next one, from 0 inclusive to 1 exclusive.
   * Rendering blends the last two steps' states by it.
   */
  public var alpha: Float = 0f
    private set

  /** Time dropped by the catch-up cap since the loop started. */
  public var droppedNanos: Long = 0
    private set

  /**
   * Runs the steps due since the last frame and returns how many ran. The first frame only starts
   * the clock.
   */
  public fun frame(): Int {
    val now = clock.nanoTime()
    if (!started) {
      started = true
      lastFrameNanos = now
      return 0
    }
    pendingNanos += now - lastFrameNanos
    lastFrameNanos = now
    var steps = 0
    while (pendingNanos >= step.stepNanos && steps < step.maxStepsPerFrame) {
      stepOnce()
      pendingNanos -= step.stepNanos
      steps++
    }
    dropBacklog()
    alpha = pendingNanos.toFloat() / step.stepNanos
    return steps
  }

  /** Runs one step now, whatever the clock says, as a host does when stepping a paused world. */
  public fun stepOnce() {
    simulation.step(tick)
    tick++
  }

  /** Drops whole steps the cap left over, keeping the part of a step already under way. */
  private fun dropBacklog() {
    if (pendingNanos < step.stepNanos) return
    val kept = pendingNanos % step.stepNanos
    droppedNanos += pendingNanos - kept
    pendingNanos = kept
  }
}
