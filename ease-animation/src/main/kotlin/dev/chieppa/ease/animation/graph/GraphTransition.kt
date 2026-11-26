package dev.chieppa.ease.animation.graph

/** A transition: from a state (-1 for any) to one, when every condition holds. */
internal class GraphTransition(
  val from: Int,
  val to: Int,
  val parameters: IntArray,
  val above: BooleanArray,
  val values: FloatArray,
  val duration: Float,
)
