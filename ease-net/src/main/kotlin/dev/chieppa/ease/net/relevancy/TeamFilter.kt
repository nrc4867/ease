package dev.chieppa.ease.net.relevancy

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World

/** [Relevance.NEVER] for an entity whose [Team] is not the viewer's; no objection otherwise. */
public object TeamFilter : RelevancyFilter {
  override fun relevance(world: World, entity: Entity, viewer: Viewer): Relevance {
    val team = with(world) { entity.getOrNull(Team) } ?: return Relevance.PASS
    return if (team.id == viewer.team) Relevance.PASS else Relevance.NEVER
  }
}
