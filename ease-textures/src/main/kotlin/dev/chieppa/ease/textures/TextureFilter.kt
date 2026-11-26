package dev.chieppa.ease.textures

/** How a texture is sampled between texels and between mip levels. */
public enum class TextureFilter {
  /** Blends neighboring texels and mip levels. */
  LINEAR,

  /** Takes the nearest texel and mip level, for pixel art. */
  NEAREST,
}
