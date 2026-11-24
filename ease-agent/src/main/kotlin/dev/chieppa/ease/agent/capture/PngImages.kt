package dev.chieppa.ease.agent.capture

import dev.chieppa.ease.gpu.Pixels
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/** Encodes frames as PNG. */
public object PngImages {
  private const val FORMAT = "png"

  /** [pixels] as PNG bytes, opaque RGB. */
  public fun encode(pixels: Pixels): ByteArray {
    val size = pixels.size
    val image = BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB)
    for (y in 0 until size.height) {
      for (x in 0 until size.width) image.setRGB(x, y, pixels.rgb(x, y))
    }
    val bytes = ByteArrayOutputStream()
    check(ImageIO.write(image, FORMAT, bytes)) { "No PNG writer is available" }
    return bytes.toByteArray()
  }
}
