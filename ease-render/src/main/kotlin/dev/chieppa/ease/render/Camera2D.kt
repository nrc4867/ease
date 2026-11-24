package dev.chieppa.ease.render

import dev.chieppa.ease.gpu.View2D
import dev.chieppa.ease.platform.PixelSize

/** World units a camera shows top to bottom unless told otherwise. */
public const val DEFAULT_VISIBLE_HEIGHT: Float = 10f

/** Clip space spans 2 units on each axis, from -1 to 1. */
private const val CLIP_SPAN = 2f

/**
 * A 2D camera looking down the z axis: centered on ([centerX], [centerY]) and showing
 * [visibleHeight] world units from the bottom of the target to the top. The width it shows follows
 * the target's aspect ratio, so nothing is stretched. Mutable, so a host moves it without
 * allocating.
 */
public class Camera2D(
  public var centerX: Float = 0f,
  public var centerY: Float = 0f,
  public var visibleHeight: Float = DEFAULT_VISIBLE_HEIGHT,
) {
  /** Sets [view] to map world positions to clip space for a target of [size] pixels. */
  public fun applyTo(view: View2D, size: PixelSize) {
    require(visibleHeight > 0f) { "A camera must show some height, got $visibleHeight" }
    val scaleY = CLIP_SPAN / visibleHeight
    val scaleX = scaleY * size.height / size.width
    view.scaleX = scaleX
    view.scaleY = scaleY
    view.offsetX = -centerX * scaleX
    view.offsetY = -centerY * scaleY
  }
}
