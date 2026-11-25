package dev.chieppa.ease.physics.body

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

/**
 * A static triangle-mesh collider at its node's transform, for generated geometry such as meshed
 * distance fields: floors and walls that are neither boxes nor spheres. Like a [Collider] its
 * [layer] decides what it collides with and its [surface] is what raycasts and characters standing
 * on it report, both by the names the game gave them, empty for the first layer and the default
 * surface. Its Jolt body is built from a copy of [mesh] when the entity gains it. Code adds it to
 * entities of its own world, on host and clients alike; it is never saved in a scene or sent to
 * peers, and an entity has either a [Collider] or a mesh collider, not both.
 */
public class MeshCollider(
  public val mesh: TriangleMesh,
  public val layer: String = "",
  public val surface: String = "",
) : Component<MeshCollider> {
  override fun type(): ComponentType<MeshCollider> = MeshCollider

  /** The component type. */
  public companion object : ComponentType<MeshCollider>()
}
