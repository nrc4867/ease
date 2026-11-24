package dev.chieppa.ease.platform

/** A window a GPU surface presents into. Closing it destroys the native window. */
public interface Window : AutoCloseable {
  /** The handles a GPU surface for this window is made from. */
  public val surface: SurfaceHandle

  /** The size of the drawable area in physical pixels. */
  public val pixelSize: PixelSize

  /** Asks the window to take [width] by [height] screen units; [pollEvents] reports the result. */
  public fun requestSize(width: Int, height: Int)
}
