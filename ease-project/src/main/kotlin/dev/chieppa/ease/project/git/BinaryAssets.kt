package dev.chieppa.ease.project.git

/**
 * The binary source formats a project keeps in Git LFS, lockable: `.gitattributes` names each, and
 * `ease check` flags one that LFS does not cover. A binary of any other extension is flagged when
 * it is larger than [MAX_PLAIN_BYTES].
 */
public object BinaryAssets {
  /** The largest binary file git may hold itself, in bytes; a larger one belongs in LFS. */
  public const val MAX_PLAIN_BYTES: Long = 1_048_576

  /** The extensions of binary asset sources: textures, audio, models, source art, fonts, video. */
  public val EXTENSIONS: List<String> =
    listOf(
      "png",
      "jpg",
      "jpeg",
      "tga",
      "bmp",
      "gif",
      "tif",
      "tiff",
      "psd",
      "exr",
      "hdr",
      "dds",
      "ktx2",
      "wav",
      "ogg",
      "mp3",
      "flac",
      "blend",
      "fbx",
      "glb",
      "ttf",
      "otf",
      "mp4",
      "webm",
    )

  /** Whether [name], a file name or path, ends in one of [EXTENSIONS]. */
  public fun hasBinaryExtension(name: String): Boolean = EXTENSIONS.any {
    name.endsWith(".$it", ignoreCase = true)
  }
}
