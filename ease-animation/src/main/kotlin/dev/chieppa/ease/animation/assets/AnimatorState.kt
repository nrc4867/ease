package dev.chieppa.ease.animation.assets

import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.assets.Guid
import kotlinx.serialization.Serializable

@Doc(
  "One state: a single clip, or clips blended along a parameter. Every clip of a state plays at " +
    "the same normalized time, so blended walks keep their feet in step."
)
@Serializable
public class AnimatorState(
  @Doc("The state's name.") public val name: String,
  @Doc("The clip asset it plays; ignored when blend is set.") public val clip: Guid? = null,
  @Doc("Clips blended along a parameter, in place of a single clip.")
  public val blend: AnimatorBlend? = null,
  @Doc("Whether it starts again when it ends; otherwise it holds its last pose.")
  public val loop: Boolean = true,
  @Doc("How fast it plays: 1 is the clips' own speed, 0 holds it; it cannot be negative.")
  public val speed: Float = 1f,
)
