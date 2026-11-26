package dev.chieppa.ease.animation.assets

import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.Serializable

@Doc(
  "A move from one state to another when every condition holds, crossfading over duration. An " +
    "empty from leaves any state but the target."
)
@Serializable
public class AnimatorTransition(
  @Doc("The state it leaves; empty for any state.") public val from: String = "",
  @Doc("The state it enters.") public val to: String,
  @Doc("What must hold, all of it, for the transition to be taken.")
  public val conditions: List<AnimatorCondition> = emptyList(),
  @Doc("How long the crossfade takes, in seconds; 0 cuts.") public val duration: Float = 0.2f,
)
