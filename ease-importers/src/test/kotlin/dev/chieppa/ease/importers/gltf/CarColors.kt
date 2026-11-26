package dev.chieppa.ease.importers.gltf

/**
 * What the imported car's texture does to a lit frame. The texture is a palette atlas, so a car
 * drawn small samples its smaller mip levels, which blend neighbouring palette cells: the body
 * comes out a mid grey, well below the white an untextured car draws.
 */
object CarColors {
  /** The brightest channel the textured body reaches; an untextured body is white. */
  private const val BODY_BRIGHTEST = 0xC0

  /** Whether [rgb] is darker than the untextured white in every channel. */
  fun isTextured(rgb: Int): Boolean =
    maxOf(rgb shr 16 and 0xFF, rgb shr 8 and 0xFF, rgb and 0xFF) < BODY_BRIGHTEST
}
