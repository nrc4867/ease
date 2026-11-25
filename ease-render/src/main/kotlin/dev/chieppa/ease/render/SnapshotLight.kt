package dev.chieppa.ease.render

import dev.chieppa.ease.render.scene.DirectionalLight

/**
 * The sun a [RenderSnapshot] is lit by: the [DirectionalLight] of the lowest entity id the last
 * extract found, its transform in the snapshot under [entity].
 */
public class SnapshotLight internal constructor() {
  /** The light's entity id, or -1 when the extract found none. */
  public var entity: Int = NONE
    internal set

  /** Whether the extract found a light. */
  public val present: Boolean
    get() = entity != NONE

  /** The light's linear color, red, green and blue. */
  public val color: FloatArray = floatArrayOf(1f, 1f, 1f)

  /** How strong the direct light is. */
  public var intensity: Float = 0f
    internal set

  /** How much of the light's color reaches every surface whatever its direction. */
  public var ambient: Float = 0f
    internal set
}
