package dev.chieppa.ease.assets.meta

import dev.chieppa.ease.serialization.format.FormatMigrations
import java.nio.file.Files
import java.nio.file.Path

/** Thrown when a meta file cannot be read; the message names the file. */
public class MetaFileException(public val file: Path, reason: String, cause: Throwable? = null) :
  RuntimeException("Meta file $file: $reason", cause)

/** Where meta files live and how they are read and written. */
public object MetaFiles {
  /**
   * The meta format version this Ease writes. Version 2 adds the optional `importer` and
   * `settings`; a version 1 file reads as a version 2 file without them and is never rewritten.
   */
  public const val FORMAT_VERSION: Int = 2

  /** The suffix that makes a file a meta file: `rock.png` is described by `rock.png.meta`. */
  public const val SUFFIX: String = ".meta"

  private val migrations = FormatMigrations(FORMAT_VERSION, mapOf(1 to { file -> file }))

  /** The meta file that describes [source]. */
  public fun metaOf(source: Path): Path = source.resolveSibling(source.fileName.toString() + SUFFIX)

  /** Whether [path] is a meta file. */
  public fun isMeta(path: Path): Boolean = path.fileName.toString().endsWith(SUFFIX)

  /** The source file the meta file [meta] describes. */
  public fun sourceOf(meta: Path): Path =
    meta.resolveSibling(meta.fileName.toString().removeSuffix(SUFFIX))

  /** The meta file at [file], migrated to the current format in memory; the file is untouched. */
  public fun read(file: Path): AssetMeta =
    VersionedYamlFile.read(file, AssetMeta.serializer(), migrations) { reason, cause ->
      MetaFileException(file, reason, cause)
    }

  /** The bytes [meta] is written as. */
  public fun encode(meta: AssetMeta): String =
    VersionedYamlFile.encode(AssetMeta.serializer(), meta)

  /**
   * Writes [meta] to [file], leaving the file alone when it already holds exactly these bytes.
   * Returns whether it wrote.
   */
  public fun write(file: Path, meta: AssetMeta): Boolean {
    val text = encode(meta)
    if (Files.exists(file) && Files.readString(file) == text) return false
    Files.writeString(file, text)
    return true
  }
}
