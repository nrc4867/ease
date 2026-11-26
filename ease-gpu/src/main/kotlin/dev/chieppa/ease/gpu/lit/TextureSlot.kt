package dev.chieppa.ease.gpu.lit

/**
 * The textures a lit material samples, glTF's metal and roughness set, in the order the texture
 * group binds them. A slot left empty samples a default that leaves the material's own values as
 * they are: white, or a flat normal.
 */
public enum class TextureSlot {
  /** Base color (sRGB) and alpha, multiplying the material's base color. */
  BASE_COLOR,

  /** A tangent-space normal map; only red and green (x and y) are read. */
  NORMAL,

  /** Roughness in green and metallic in blue, multiplying the material's factors. */
  METALLIC_ROUGHNESS,

  /** Emitted light (sRGB), multiplying the material's emissive color. */
  EMISSIVE,

  /** Ambient occlusion in red, darkening ambient light. */
  OCCLUSION,
}
