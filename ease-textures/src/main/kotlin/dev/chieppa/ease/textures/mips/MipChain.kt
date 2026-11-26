package dev.chieppa.ease.textures.mips

import dev.chieppa.ease.platform.images.TextureSizes
import dev.chieppa.ease.textures.TextureUsage

/** Builds a texture's mip levels with [AreaResize], each from the one before. */
public object MipChain {
  /** [base] and, when [mipmaps], every smaller level down to 1 by 1. */
  public fun build(base: LinearImage, usage: TextureUsage, mipmaps: Boolean): List<LinearImage> {
    val count = if (mipmaps) TextureSizes.levelCount(base.width, base.height) else 1
    val levels = ArrayList<LinearImage>(count)
    levels += base
    while (levels.size < count) levels += AreaResize.half(levels.last(), usage)
    return levels
  }

  /** [image] halved until neither side is over [maxSize]. */
  public fun fit(image: LinearImage, maxSize: Int, usage: TextureUsage): LinearImage {
    var fitted = image
    while (fitted.width > maxSize || fitted.height > maxSize) {
      fitted = AreaResize.half(fitted, usage)
    }
    return fitted
  }
}
