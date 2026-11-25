package dev.chieppa.ease.physics.query

import com.github.quillraven.fleks.Entity

/**
 * What a raycast hit, filled in place by `PhysicsWorld.raycast` so casting makes no objects: keep
 * one and pass it to every cast. Valid only after a cast that returned true.
 */
public class RayHit {
  /** The entity whose collider was hit. */
  public var entity: Entity = Entity.NONE
    internal set

  /** How far along the ray the hit is, in world units. */
  public var distance: Float = 0f
    internal set

  /** The hit point along x. */
  public var x: Float = 0f
    internal set

  /** The hit point along y. */
  public var y: Float = 0f
    internal set

  /** The hit point along z. */
  public var z: Float = 0f
    internal set

  /** The index of the hit collider's surface type in the world's `SurfaceTypes`. */
  public var surface: Int = 0
    internal set
}
