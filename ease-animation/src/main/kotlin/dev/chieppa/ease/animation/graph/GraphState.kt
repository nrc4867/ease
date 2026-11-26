package dev.chieppa.ease.animation.graph

/**
 * One state: the graph clips it plays ([clips], indices into the graph's), along [parameter] at
 * [points] when it blends (-1 and one clip when it does not).
 */
internal class GraphState(
  val name: String,
  val clips: IntArray,
  val points: FloatArray,
  val parameter: Int,
  val loop: Boolean,
  val speed: Float,
)
