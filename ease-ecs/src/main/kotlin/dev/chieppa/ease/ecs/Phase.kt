package dev.chieppa.ease.ecs

/** The fixed stages of one tick, in the order they run. */
public enum class Phase {
  /** Reads devices and commands into components. */
  INPUT,
  /** Game rules and movement. */
  SIMULATION,
  /** Collision and forces. */
  PHYSICS,
  /** Poses, blends and timelines. */
  ANIMATION,
  /** Copies what the renderer needs out of the world. */
  EXTRACT,
}
