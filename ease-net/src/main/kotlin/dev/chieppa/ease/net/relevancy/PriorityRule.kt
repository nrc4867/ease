package dev.chieppa.ease.net.relevancy

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World

/**
 * How urgently an entity with changes should reach a peer this tick, when a snapshot cannot carry
 * every one. Entities go in order of their score plus what they built up while left out, so one
 * left out long enough always gets in. Rules read the world and allocate nothing.
 */
public fun interface PriorityRule {
  /**
   * [entity]'s score for [viewer] this tick, at least 0; [speed] is how far its `LocalTransform`
   * moved since the last tick, 0 without one.
   */
  public fun priority(world: World, entity: Entity, viewer: Viewer, speed: Float): Float
}
