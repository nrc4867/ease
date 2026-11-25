package dev.chieppa.ease.assets.presets

import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * The contents of a folder's preset file, `.import-preset.yaml`: the importer and settings that
 * apply to every asset under the folder, however deep. Sidecars win over presets, and a nearer
 * folder's preset wins over a farther one's, key by key.
 */
@Doc("A folder's import preset: the importer and settings for everything under the folder.")
@Serializable
public class FolderPreset(
  @Doc("The preset format version this file is written in.")
  public val formatVersion: Int = FolderPresets.FORMAT_VERSION,
  @Doc("The id of the importer for files under the folder, instead of the one their type matches.")
  public val importer: String? = null,
  @Doc("Import settings for files under the folder.") public val settings: JsonObject? = null,
)
