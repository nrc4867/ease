package dev.chieppa.ease.assets.meta

import dev.chieppa.ease.serialization.format.FormatMigrations
import dev.chieppa.ease.serialization.format.UnsupportedFormatVersion
import dev.chieppa.ease.serialization.yaml.CanonicalYaml
import dev.chieppa.ease.serialization.yaml.YamlFormatException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject

/** Thrown when a meta file cannot be read; the message names the file. */
public class MetaFileException(public val file: Path, reason: String, cause: Throwable? = null) :
  RuntimeException("Meta file $file: $reason", cause)

/** Where meta files live and how they are read and written. */
public object MetaFiles {
  /** The meta format version this Ease writes. */
  public const val FORMAT_VERSION: Int = 1

  /** The suffix that makes a file a meta file: `rock.png` is described by `rock.png.meta`. */
  public const val SUFFIX: String = ".meta"

  private val migrations = FormatMigrations(FORMAT_VERSION)

  /** The meta file that describes [source]. */
  public fun metaOf(source: Path): Path = source.resolveSibling(source.fileName.toString() + SUFFIX)

  /** Whether [path] is a meta file. */
  public fun isMeta(path: Path): Boolean = path.fileName.toString().endsWith(SUFFIX)

  /** The source file the meta file [meta] describes. */
  public fun sourceOf(meta: Path): Path =
    meta.resolveSibling(meta.fileName.toString().removeSuffix(SUFFIX))

  /** The meta file at [file], migrated to the current format in memory; the file is untouched. */
  public fun read(file: Path): AssetMeta {
    val failure: Exception =
      try {
        return parse(file)
      } catch (failure: YamlFormatException) {
        failure
      } catch (failure: UnsupportedFormatVersion) {
        failure
      } catch (failure: SerializationException) {
        failure
      } catch (failure: IllegalArgumentException) {
        failure
      } catch (failure: IOException) {
        failure
      }
    throw MetaFileException(file, failure.message.orEmpty(), failure)
  }

  private fun parse(file: Path): AssetMeta {
    val tree = CanonicalYaml.read(Files.readString(file))
    val mapping = tree as? JsonObject ?: throw YamlFormatException("not a mapping")
    val migrated = migrations.migrate(mapping)
    return CanonicalYaml.decode(AssetMeta.serializer(), CanonicalYaml.write(migrated))
  }

  /** The bytes [meta] is written as. */
  public fun encode(meta: AssetMeta): String = CanonicalYaml.encode(AssetMeta.serializer(), meta)

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
