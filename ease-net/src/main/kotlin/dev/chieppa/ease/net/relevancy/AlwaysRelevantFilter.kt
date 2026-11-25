package dev.chieppa.ease.net.relevancy

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World

/** [Relevance.ALWAYS] for an entity carrying [AlwaysRelevant]; no objection to any other. */
public object AlwaysRelevantFilter : RelevancyFilter {
  override fun relevance(world: World, entity: Entity, viewer: Viewer): Relevance =
    if (with(world) { entity.has(AlwaysRelevant) }) Relevance.ALWAYS else Relevance.PASS
}
