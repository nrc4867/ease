package dev.chieppa.ease.animation.assets

import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.Serializable

@Doc("How a condition compares its parameter with its value.")
@Serializable
public enum class Comparison {
  @Doc("Holds when the parameter is greater than the value.") ABOVE,
  @Doc("Holds when the parameter is less than the value.") BELOW,
}
