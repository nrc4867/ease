package dev.chieppa.ease.importers.gltf

import dev.chieppa.ease.textures.TextureUsage

/**
 * Turns an encoded image (PNG or JPEG) into a cooked texture's bytes; the engine's is `cookTexture`
 * from `ease-textures`.
 */
public fun interface TextureCooker {
  /** The cooked texture of [image], whose type is [mimeType], for [usage]. */
  public fun cook(image: ByteArray, mimeType: String, usage: TextureUsage): ByteArray
}
