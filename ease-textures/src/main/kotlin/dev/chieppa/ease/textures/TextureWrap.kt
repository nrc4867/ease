package dev.chieppa.ease.textures

/** What a texture coordinate outside 0 to 1 samples. */
public enum class TextureWrap {
  /** The texture tiles. */
  REPEAT,

  /** The edge texels stretch outward. */
  CLAMP,

  /** The texture tiles, every other copy mirrored. */
  MIRROR,
}
