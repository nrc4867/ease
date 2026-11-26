package dev.chieppa.ease.gpu.texture

/** What a sampler reads for texture coordinates outside 0 to 1. */
public enum class SamplerWrap {
  /** The texture tiles. */
  REPEAT,

  /** The edge texels stretch outward. */
  CLAMP,

  /** The texture tiles, every other copy mirrored. */
  MIRROR,
}
