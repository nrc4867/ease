package dev.chieppa.ease.runtime.loop

/**
 * The length of one simulation step, and how many steps one frame may run to catch up after a slow
 * frame. Time past that cap is dropped, so a loop can never spiral into ever more steps.
 */
public class FixedStep(public val stepNanos: Long, public val maxStepsPerFrame: Int) {
  init {
    require(stepNanos > 0) { "A step must last longer than 0 ns, got $stepNanos" }
    require(maxStepsPerFrame > 0) { "A frame must run at least 1 step, got $maxStepsPerFrame" }
  }

  public companion object {
    private const val NANOS_PER_SECOND = 1_000_000_000L

    /** The default simulation rate. */
    public const val DEFAULT_STEPS_PER_SECOND: Int = 60

    /** The default catch-up cap: a quarter of a second at the default rate. */
    public const val DEFAULT_MAX_STEPS_PER_FRAME: Int = 15

    /** [stepsPerSecond] steps a second, each [NANOS_PER_SECOND] / [stepsPerSecond] long. */
    public fun perSecond(
      stepsPerSecond: Int = DEFAULT_STEPS_PER_SECOND,
      maxStepsPerFrame: Int = DEFAULT_MAX_STEPS_PER_FRAME,
    ): FixedStep {
      require(stepsPerSecond > 0) { "Needs at least 1 step a second, got $stepsPerSecond" }
      return FixedStep(NANOS_PER_SECOND / stepsPerSecond, maxStepsPerFrame)
    }
  }
}
