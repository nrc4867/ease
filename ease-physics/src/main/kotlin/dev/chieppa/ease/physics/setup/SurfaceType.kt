package dev.chieppa.ease.physics.setup

/**
 * A kind of surface a game declares (stone, grass, ice) with what physics uses of it: [friction]
 * and [restitution] for bodies touching it. Raycasts and characters report it by its index in
 * [SurfaceTypes], so a game keeps its own data per surface, such as footstep sounds, by that index.
 */
public data class SurfaceType(
  /** The name colliders give in their `surface` field. */
  val name: String,
  /** How much it resists sliding, from 0 (ice); Jolt's default is 0.2. */
  val friction: Float = DEFAULT_FRICTION,
  /** How much it bounces, from 0 (not at all) to 1. */
  val restitution: Float = 0f,
) {
  public companion object {
    /** The friction of a surface that names none. */
    public const val DEFAULT_FRICTION: Float = 0.2f
  }
}
