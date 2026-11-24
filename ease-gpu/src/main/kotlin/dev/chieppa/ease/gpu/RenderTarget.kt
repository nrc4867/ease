package dev.chieppa.ease.gpu

import dev.chieppa.ease.platform.PixelSize
import java.lang.foreign.MemorySegment

/**
 * A color texture view a pass draws into, its [size] and [format]: a window's current frame, an
 * offscreen texture, or a host's own texture handed over by address.
 */
public class RenderTarget
internal constructor(
  internal val view: MemorySegment,
  public val size: PixelSize,
  public val format: TextureFormat,
) {
  public companion object {
    /**
     * A target over a texture view someone else owns, such as an editor viewport's frame, given as
     * its wgpu handle address. The owner keeps it alive while passes draw into it.
     */
    public fun borrowed(view: Long, size: PixelSize, format: TextureFormat): RenderTarget =
      RenderTarget(MemorySegment.ofAddress(view), size, format)
  }
}
