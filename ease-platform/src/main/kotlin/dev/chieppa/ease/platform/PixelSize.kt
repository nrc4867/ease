package dev.chieppa.ease.platform

/** A size in physical pixels, which is what a GPU surface is configured in. */
public data class PixelSize(val width: Int, val height: Int) {
  /** Whether nothing can be drawn at this size, as while a window is minimized. */
  val isEmpty: Boolean
    get() = width <= 0 || height <= 0
}
