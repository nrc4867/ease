package dev.chieppa.ease.assets.meta

import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.assets.Guid
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * What a meta file holds, the `.meta` file beside a source file: the asset's identity and, only
 * when someone chose them, its importer and import settings. Nothing in it changes when the source
 * is edited, so two people editing one source never conflict on it.
 */
@Doc("The contents of a meta file: the asset's GUID and any import choices.")
@Serializable
public class AssetMeta(
  @Doc("The meta format version this file is written in.")
  public val formatVersion: Int = MetaFiles.FORMAT_VERSION,
  @Doc("The asset's identity; it stays the same when the source file is moved or renamed.")
  public val guid: Guid,
  @Doc("The id of the importer that imports this file, instead of the one its type matches.")
  public val importer: String? = null,
  @Doc("Import settings for this file; they win over the settings of folder presets.")
  public val settings: JsonObject? = null,
) {
  /** This meta file's contents with another GUID. */
  public fun withGuid(guid: Guid): AssetMeta = AssetMeta(formatVersion, guid, importer, settings)
}
