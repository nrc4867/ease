package dev.chieppa.ease.gpu

import dev.chieppa.ease.platform.PixelSize

/**
 * A color texture view a pass draws into, its [size] and [format]: a window's current frame, an
 * offscreen texture, or a host's own texture handed over by address.
 */
public class RenderTarget
internal constructor(
  /** The wgpu texture view's address. */
  internal var view: Long,
  size: PixelSize,
  public val format: TextureFormat,
) {
  /** The target's size in pixels. */
  public var size: PixelSize = size
    internal set

  public companion object {
    /**
     * A target over a texture view someone else owns, such as an editor viewport's frame, given as
     * its wgpu handle address. The owner keeps it alive while passes draw into it.
     */
    public fun borrowed(view: Long, size: PixelSize, format: TextureFormat): RenderTarget =
      RenderTarget(view, size, format)
  }
}
