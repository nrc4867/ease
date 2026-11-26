package dev.chieppa.ease.animation.assets

import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.Serializable

@Doc("What sets an animator parameter.")
@Serializable
public enum class ParameterDrive {
  @Doc("The game's code sets it through the world's Animators.") GAME,
  @Doc(
    "The animated node's speed across the ground: how far its world position moved along x and z " +
      "in the last tick, in world units a second."
  )
  MOTION_SPEED,
}
