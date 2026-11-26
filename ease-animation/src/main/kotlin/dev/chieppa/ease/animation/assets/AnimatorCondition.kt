package dev.chieppa.ease.animation.assets

import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.Serializable

@Doc("A test of one parameter against a value.")
@Serializable
public class AnimatorCondition(
  @Doc("The parameter tested.") public val parameter: String,
  @Doc("How it is compared.") public val test: Comparison,
  @Doc("The value it is compared with.") public val value: Float,
)
