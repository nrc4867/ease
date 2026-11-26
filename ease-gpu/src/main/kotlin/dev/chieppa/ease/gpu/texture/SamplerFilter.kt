package dev.chieppa.ease.gpu.texture

/** How a sampler reads between texels and mip levels. */
public enum class SamplerFilter {
  /** Blends neighboring texels and levels. */
  LINEAR,

  /** Takes the nearest texel and level. */
  NEAREST,
}
