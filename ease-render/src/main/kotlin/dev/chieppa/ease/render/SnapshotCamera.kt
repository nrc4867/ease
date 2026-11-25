package dev.chieppa.ease.render

import dev.chieppa.ease.render.scene.Camera

/**
 * The camera a [RenderSnapshot] is seen through: the [Camera] of the lowest entity id the last
 * extract found, its transform in the snapshot under [entity].
 */
public class SnapshotCamera internal constructor() {
  /** The camera's entity id, or -1 when the extract found none. */
  public var entity: Int = NONE
    internal set

  /** Whether the extract found a camera. */
  public val present: Boolean
    get() = entity != NONE

  /** The vertical field of view, in degrees. */
  public var fieldOfView: Float = Camera.DEFAULT_FIELD_OF_VIEW
    internal set

  /** The nearest distance drawn. */
  public var near: Float = Camera.DEFAULT_NEAR
    internal set

  /** The farthest distance drawn. */
  public var far: Float = Camera.DEFAULT_FAR
    internal set
}
