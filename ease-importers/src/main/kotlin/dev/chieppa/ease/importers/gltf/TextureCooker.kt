package dev.chieppa.ease.importers.gltf

/** What a material uses an image for, which decides how its texture is compressed. */
public enum class ImageUsage {
  /** Colors stored in sRGB: base color, emission. */
  COLOR_SRGB,

  /** Linear data: metallic-roughness, occlusion. */
  DATA_LINEAR,

  /** A tangent-space normal map. */
  NORMAL,
}

/** Turns an encoded image (PNG or JPEG) into a cooked texture's bytes. */
public fun interface TextureCooker {
  /** The cooked texture of [image], whose type is [mimeType], for [usage]. */
  public fun cook(image: ByteArray, mimeType: String, usage: ImageUsage): ByteArray
}
