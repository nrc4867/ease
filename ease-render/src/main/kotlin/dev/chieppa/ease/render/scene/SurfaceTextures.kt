package dev.chieppa.ease.render.scene

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.gpu.lit.MaterialTextures

/**
 * The textures its entity's mesh is drawn with: a [MaterialTextures] set made on the renderer's
 * device. Without it a mesh samples the defaults, which leave its [Material] as it is. Code adds it
 * to entities of its own world; it is never saved in a scene or sent to peers.
 */
public class SurfaceTextures(public val textures: MaterialTextures) : Component<SurfaceTextures> {
  override fun type(): ComponentType<SurfaceTextures> = SurfaceTextures

  /** The component type. */
  public companion object : ComponentType<SurfaceTextures>()
}
