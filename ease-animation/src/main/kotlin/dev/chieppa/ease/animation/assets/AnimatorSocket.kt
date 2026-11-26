package dev.chieppa.ease.animation.assets

import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.Serializable

@Doc("A named place on a bone, offset in the bone's own space, that nodes and probes can follow.")
@Serializable
public class AnimatorSocket(
  @Doc("The socket's name.") public val name: String,
  @Doc("The bone it rides on, by node name.") public val bone: String,
  @Doc("Offset along the bone's x, in its units.") public val x: Float = 0f,
  @Doc("Offset along the bone's y, in its units.") public val y: Float = 0f,
  @Doc("Offset along the bone's z, in its units.") public val z: Float = 0f,
)
