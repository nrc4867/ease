package dev.chieppa.ease.platform

/**
 * What window to open: its [title] and its size in screen units, which a high-density display
 * scales.
 */
public data class WindowSpec(
  val title: String,
  val width: Int,
  val height: Int,
  val resizable: Boolean = true,
)
