package dev.chieppa.ease.animation.assets

import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.assets.Guid
import kotlinx.serialization.Serializable

@Doc("One clip of a blend and where it sits along the blend's parameter.")
@Serializable
public class AnimatorBlendPoint(
  @Doc("The clip asset.") public val clip: Guid,
  @Doc("The parameter value at which it has full weight.") public val at: Float,
)
