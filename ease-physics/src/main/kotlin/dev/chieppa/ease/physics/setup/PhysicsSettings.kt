package dev.chieppa.ease.physics.setup

/**
 * What a world's physics is built with: the game's collision [layers] and [surfaces], gravity along
 * y ([gravityY], world units a second squared), how many bodies it holds at most ([maxBodies]) and
 * how many worker threads step it ([workerThreads]; 0 steps it on the calling thread).
 */
public data class PhysicsSettings(
  val layers: CollisionLayers = CollisionLayers.build(),
  val surfaces: SurfaceTypes = SurfaceTypes.of(),
  val gravityY: Float = DEFAULT_GRAVITY,
  val maxBodies: Int = DEFAULT_MAX_BODIES,
  val workerThreads: Int = 0,
) {
  public companion object {
    /** Earth's gravity, downward, in world units (meters) a second squared. */
    public const val DEFAULT_GRAVITY: Float = -9.81f

    /** How many bodies a world holds unless told otherwise. */
    public const val DEFAULT_MAX_BODIES: Int = 4096
  }
}
