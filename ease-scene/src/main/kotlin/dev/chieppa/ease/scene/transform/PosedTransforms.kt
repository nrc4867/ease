package dev.chieppa.ease.scene.transform

import com.github.quillraven.fleks.Entity

/**
 * Places nodes some other way than their `LocalTransform`, such as the bones of an animated model,
 * whose pose is worked out every tick and never written to their components. [WorldTransforms] asks
 * it for every node it places; it answers with one of [UNPOSED], [LOCAL] or [WORLD]. It must not
 * allocate.
 */
public fun interface PosedTransforms {
  /**
   * Writes [entity]'s transform, [WORLD_TRANSFORM_FLOATS] floats, into [dst] at [at] and returns
   * [LOCAL] when it is relative to the node's parent or [WORLD] when it is already in the world;
   * returns [UNPOSED], writing nothing, when the node is placed by its `LocalTransform`.
   */
  public fun write(entity: Entity, dst: FloatArray, at: Int): Int

  public companion object {
    /** The node is placed by its `LocalTransform`. */
    public const val UNPOSED: Int = 0

    /** The transform written is relative to the node's parent. */
    public const val LOCAL: Int = 1

    /** The transform written is the node's place in the world; its parent is not applied. */
    public const val WORLD: Int = 2
  }
}
