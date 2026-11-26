package dev.chieppa.ease.animation.assets

import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.Serializable

@Doc(
  "A point on a bone or socket that asks the physics world for the surface below it whenever its " +
    "clip event fires, such as a foot's probe on its footstep event. Each run is reported to the " +
    "world's AnimationEvents with the surface found."
)
@Serializable
public class AnimatorProbe(
  @Doc("The probe's name, such as leftFoot.") public val name: String,
  @Doc("The socket, or else the bone by node name, it sits on.") public val at: String,
  @Doc("The clip event that runs it.") public val event: String,
  @Doc("How far above the point the downward ray starts, in world units.")
  public val lift: Float = DEFAULT_LIFT,
  @Doc("How far below the point the ray still finds ground, in world units.")
  public val depth: Float = DEFAULT_DEPTH,
) {
  public companion object {
    /** How far above its point a probe's ray starts unless told otherwise, in world units. */
    public const val DEFAULT_LIFT: Float = 0.25f

    /** How far below its point a probe finds ground unless told otherwise, in world units. */
    public const val DEFAULT_DEPTH: Float = 0.5f
  }
}
