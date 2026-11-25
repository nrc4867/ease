package dev.chieppa.ease.net.relevancy

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.replication.NetworkId
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.ecs.replication.entityOf

/** The network id of no entity: a viewer without a focus. */
private const val NO_FOCUS = 0L

/**
 * Where one peer looks at the world from, which relevancy filters and priority read: a position, a
 * team and an area. With a focus, such as the peer's own character, each tick takes the focus's
 * `LocalTransform`, [Team] and [Area]; without one, game code sets them.
 */
public class Viewer
internal constructor(
  /** The peer that looks. */
  public val peer: PeerId
) {
  /** The viewer's position along x. */
  public var x: Float = 0f

  /** The viewer's position along y. */
  public var y: Float = 0f

  /** The viewer's position along z. */
  public var z: Float = 0f

  /** The viewer's team, or [Team.NONE]. */
  public var team: Int = Team.NONE

  /** The viewer's area, or [Area.NONE]. */
  public var area: Int = Area.NONE

  private var focusId = NO_FOCUS

  /** Follows [entity] from the next tick on: its position, team and area become the viewer's. */
  public fun focus(entity: NetworkId) {
    focusId = entity.value
  }

  /** Stops following an entity; the viewer keeps the values it last took. */
  public fun clearFocus() {
    focusId = NO_FOCUS
  }

  /** The squared distance from the viewer to [transform]'s position. */
  public fun distanceSquaredTo(transform: LocalTransform): Float {
    val dx = transform.x - x
    val dy = transform.y - y
    val dz = transform.z - z
    return dx * dx + dy * dy + dz * dz
  }

  /** Takes the focus's position, team and area, when the focus is a live entity of [world]. */
  internal fun follow(world: World) {
    if (focusId == NO_FOCUS) return
    val entity = world.entityOf(NetworkId(focusId))
    if (entity == Entity.NONE) return
    with(world) {
      entity.getOrNull(LocalTransform)?.let {
        x = it.x
        y = it.y
        z = it.z
      }
      team = entity.getOrNull(Team)?.id ?: Team.NONE
      area = entity.getOrNull(Area)?.id ?: Area.NONE
    }
  }
}
