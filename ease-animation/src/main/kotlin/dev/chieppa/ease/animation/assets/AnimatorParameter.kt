package dev.chieppa.ease.animation.assets

import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.Serializable

@Doc("A number states and transitions read, such as speed.")
@Serializable
public class AnimatorParameter(
  @Doc("The parameter's name.") public val name: String,
  @Doc("What sets it: the game's code, or the node's own movement.")
  public val drive: ParameterDrive = ParameterDrive.GAME,
  @Doc("Its value before anything sets it.") public val initial: Float = 0f,
)
