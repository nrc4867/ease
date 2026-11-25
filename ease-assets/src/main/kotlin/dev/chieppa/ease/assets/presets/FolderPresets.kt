package dev.chieppa.ease.assets.presets

import dev.chieppa.ease.assets.meta.VersionedYamlFile
import dev.chieppa.ease.serialization.format.FormatMigrations
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.JsonObject

/** Thrown when a preset file cannot be read; the message names the file. */
public class PresetFileException(public val file: Path, reason: String, cause: Throwable? = null) :
  RuntimeException("Preset file $file: $reason", cause)

/** What presets say about one asset: the importer and the settings of every folder above it. */
public class PresetChoice(
  /** The importer id of the nearest preset that names one, or null. */
  public val importer: String?,
  /** The settings of every preset above the asset, nearer folders winning key by key. */
  public val settings: JsonObject,
)

/** Where folder presets live and how they are read. */
public object FolderPresets {
  /** The preset format version this Ease writes. */
  public const val FORMAT_VERSION: Int = 1

  /** The name of a preset file; a dot file, so it is never an asset. */
  public const val FILE_NAME: String = ".import-preset.yaml"

  private val migrations = FormatMigrations(FORMAT_VERSION)

  /** The preset file at [file], migrated in memory; the file is untouched. */
  public fun read(file: Path): FolderPreset =
    VersionedYamlFile.read(file, FolderPreset.serializer(), migrations) { reason, cause ->
      PresetFileException(file, reason, cause)
    }

  /** The bytes [preset] is written as. */
  public fun encode(preset: FolderPreset): String =
    VersionedYamlFile.encode(FolderPreset.serializer(), preset)

  /**
   * What the presets in the folders from [root] down to the one holding [source] (a path relative
   * to [root]) say. Throws [PresetFileException] for a preset that cannot be read.
   */
  public fun choiceFor(root: Path, source: Path): PresetChoice {
    var importer: String? = null
    var settings = JsonObject(emptyMap())
    var folder = root
    val parents = (0 until source.nameCount - 1).map { source.getName(it) }
    for (name in listOf<Path?>(null) + parents) {
      if (name != null) folder = folder.resolve(name)
      val file = folder.resolve(FILE_NAME)
      if (!Files.isRegularFile(file)) continue
      val preset = read(file)
      importer = preset.importer ?: importer
      settings = JsonObject(settings + preset.settings.orEmpty())
    }
    return PresetChoice(importer, settings)
  }
}
