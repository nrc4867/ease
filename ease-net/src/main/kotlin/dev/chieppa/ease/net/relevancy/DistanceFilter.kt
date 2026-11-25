package dev.chieppa.ease.net.relevancy

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.ecs.LocalTransform

/**
 * [Relevance.NEVER] for an entity whose `LocalTransform` lies farther than [radius] from the
 * viewer; no objection to one nearer or without a transform. Positions are compared as the
 * transforms hold them, so nested nodes compare their offsets from their parents.
 */
public class DistanceFilter(public val radius: Float) : RelevancyFilter {
  private val radiusSquared = radius * radius

  init {
    require(radius >= 0f && radius.isFinite()) { "A radius is finite and not negative: $radius" }
  }

  override fun relevance(world: World, entity: Entity, viewer: Viewer): Relevance {
    val transform = with(world) { entity.getOrNull(LocalTransform) } ?: return Relevance.PASS
    val near = viewer.distanceSquaredTo(transform) <= radiusSquared
    return if (near) Relevance.PASS else Relevance.NEVER
  }
}
