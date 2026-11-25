package dev.chieppa.ease.net.relevancy

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World

/** [Relevance.NEVER] for an entity whose [Area] is not the viewer's; no objection otherwise. */
public object AreaFilter : RelevancyFilter {
  override fun relevance(world: World, entity: Entity, viewer: Viewer): Relevance {
    val area = with(world) { entity.getOrNull(Area) } ?: return Relevance.PASS
    return if (area.id == viewer.area) Relevance.PASS else Relevance.NEVER
  }
}
