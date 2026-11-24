package dev.chieppa.ease.gpu

/**
 * How a 2D pass maps world positions to clip space: `clip = world * scale + offset` on each axis.
 * Mutable, so a renderer sets one per frame without allocating.
 */
public class View2D {
  public var scaleX: Float = 1f
  public var scaleY: Float = 1f
  public var offsetX: Float = 0f
  public var offsetY: Float = 0f
}
