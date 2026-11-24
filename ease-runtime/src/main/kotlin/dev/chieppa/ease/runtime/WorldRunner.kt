package dev.chieppa.ease.runtime

import dev.chieppa.ease.platform.time.Clock
import dev.chieppa.ease.runtime.loop.FixedStepLoop
import dev.chieppa.ease.runtime.loop.Simulation

/**
 * Runs [world] on the standard fixed-step loop from [clock]. A host calls [frame] once per frame,
 * then renders [world]'s snapshot blended by [alpha]. Nothing here allocates per frame.
 */
public class WorldRunner(public val world: HostedWorld, clock: Clock) {
  private val loop = FixedStepLoop(clock, world.fixedStep, Simulation { world.step() })

  /** How far the clock is between the last tick and the next, from 0 inclusive to 1 exclusive. */
  public val alpha: Float
    get() = loop.alpha

  /** Runs the ticks due since the last frame and returns how many ran. The first only starts. */
  public fun frame(): Int = loop.frame()

  /** Runs one tick now, whatever the clock says, as a host stepping a paused world does. */
  public fun stepOnce() {
    loop.stepOnce()
  }
}
