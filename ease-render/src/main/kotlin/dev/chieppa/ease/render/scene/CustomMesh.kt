package dev.chieppa.ease.render.scene

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.gpu.mesh.MeshData

/**
 * Draws a mesh built at runtime, such as generated level geometry or debug shapes, at its entity's
 * LocalTransform, in place of any [Mesh]. Code adds it to entities of its own world; it is never
 * saved in a scene or sent to peers. Rebuilding [mesh] and committing it changes what the next
 * frame draws.
 */
public class CustomMesh(public val mesh: MeshData) : Component<CustomMesh> {
  override fun type(): ComponentType<CustomMesh> = CustomMesh

  /** The component type. */
  public companion object : ComponentType<CustomMesh>()
}
