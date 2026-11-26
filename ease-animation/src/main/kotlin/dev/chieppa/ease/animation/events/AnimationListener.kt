package dev.chieppa.ease.animation.events

import com.github.quillraven.fleks.Entity

/**
 * Receives what a world's animators report, during the tick that played it, through
 * [AnimationEvents]. Audio, effects and scripts subscribe here; games may too. Each method does
 * nothing unless overridden. Entities are the world's own, valid for the call; names are the
 * assets' own strings.
 */
public interface AnimationListener {
  /** Playback of [animator]'s clip passed its event [event], such as a foot coming down. */
  public fun clipEvent(animator: Entity, event: String) {}

  /**
   * [animator]'s probe [probe] ran, at ([x], [y], [z]) in the world, and found the surface type at
   * index [surface] below it, or -1 when no ground is within its reach or the world has no physics.
   */
  @Suppress("LongParameterList") // The point as three floats, so reporting allocates nothing.
  public fun probe(animator: Entity, probe: String, surface: Int, x: Float, y: Float, z: Float) {}
}
