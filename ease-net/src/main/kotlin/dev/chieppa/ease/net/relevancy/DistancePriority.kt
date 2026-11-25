package dev.chieppa.ease.net.relevancy

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.ecs.LocalTransform
import kotlin.math.sqrt

/**
 * Scores nearby fast entities over distant still ones: `(1 + speed × speedWeight) / (1 + distance /
 * distanceScale)`, so an entity [distanceScale] away scores half what one at the viewer does, and
 * one moving `1 / speedWeight` a tick scores twice what a still one does. An entity without a
 * `LocalTransform` scores as one at the viewer.
 */
public class DistancePriority(
  public val distanceScale: Float = DEFAULT_DISTANCE_SCALE,
  public val speedWeight: Float = DEFAULT_SPEED_WEIGHT,
) : PriorityRule {
  init {
    require(distanceScale > 0f) { "A distance scale is above 0: $distanceScale" }
    require(speedWeight >= 0f) { "A speed weight is not negative: $speedWeight" }
  }

  override fun priority(world: World, entity: Entity, viewer: Viewer, speed: Float): Float {
    val transform = with(world) { entity.getOrNull(LocalTransform) }
    val distance = if (transform == null) 0f else sqrt(viewer.distanceSquaredTo(transform))
    return (1f + speed * speedWeight) / (1f + distance / distanceScale)
  }

  public companion object {
    /** The distance at which an entity's score halves, in world units. */
    public const val DEFAULT_DISTANCE_SCALE: Float = 10f

    /** How much each world unit moved a tick adds to an entity's score. */
    public const val DEFAULT_SPEED_WEIGHT: Float = 4f
  }
}
