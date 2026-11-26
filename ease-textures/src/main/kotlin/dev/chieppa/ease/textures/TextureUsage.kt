package dev.chieppa.ease.textures

/** What a texture's pixels mean, which decides how it is filtered, compressed and sampled. */
public enum class TextureUsage {
  /** Colors stored in sRGB, such as a base color: mipmapped in linear light, sampled as sRGB. */
  COLOR_SRGB,

  /** Values that are not colors, such as metal and roughness or occlusion: kept linear. */
  DATA_LINEAR,

  /**
   * A tangent-space normal map: only red and green (x and y) are kept, z is rebuilt when sampled.
   */
  NORMAL,
}
