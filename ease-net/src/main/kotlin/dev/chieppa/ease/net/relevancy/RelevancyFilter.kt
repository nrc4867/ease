package dev.chieppa.ease.net.relevancy

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World

/**
 * One test of whether an entity matters to a peer, run for every entity and peer each tick. An
 * entity is relevant when a filter says [Relevance.ALWAYS], or when none says [Relevance.NEVER] or
 * [Relevance.DISTANT]; the peer that owns an entity always finds it relevant. Filters read the
 * world and allocate nothing.
 */
public fun interface RelevancyFilter {
  /** What this filter says about [entity] of [world] for [viewer]. */
  public fun relevance(world: World, entity: Entity, viewer: Viewer): Relevance
}
