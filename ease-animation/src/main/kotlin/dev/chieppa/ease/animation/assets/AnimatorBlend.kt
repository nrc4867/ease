package dev.chieppa.ease.animation.assets

import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.Serializable

@Doc(
  "Clips placed along one parameter: each clip has full weight at its point, and between two " +
    "points the two nearest share the weight linearly. Past either end the end clip plays alone."
)
@Serializable
public class AnimatorBlend(
  @Doc("The parameter that picks the blend.") public val parameter: String,
  @Doc("The clips and where each sits along the parameter, in increasing order.")
  public val points: List<AnimatorBlendPoint>,
)
