package dev.chieppa.ease.assets.importing

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.memory.MappedFile

/**
 * What an importer may know and read beyond its source's bytes. Every file read through it is
 * recorded with its contents hash, and the import's cache key covers them, so a change to one
 * imports the source again. Files are named relative to the source's folder and must stay inside
 * the content folder.
 */
public interface ImportContext {
  /** The source's GUID; a cooked asset's sub-asset GUID is `guid.named(cookedName)`. */
  public val guid: Guid

  /** The source's file name, such as `crate.gltf`. */
  public val sourceName: String

  /** The bytes of the file at [relativePath]. Throws [ImportException] when it cannot be read. */
  public fun read(relativePath: String): ByteArray

  /**
   * The file at [relativePath] mapped read-only; it is unmapped once the import returns. Throws
   * [ImportException] when it cannot be read.
   */
  public fun map(relativePath: String): MappedFile
}
