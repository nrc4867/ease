package dev.chieppa.ease.runtime.loop

/** What a loop advances: one call is one fixed step, numbered by its tick. */
public fun interface Simulation {
  /** Advances one fixed step. [tick] counts from 0 and is the only time the simulation sees. */
  public fun step(tick: Long)
}
