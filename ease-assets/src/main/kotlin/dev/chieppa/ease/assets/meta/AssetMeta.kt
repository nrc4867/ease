package dev.chieppa.ease.assets.meta

import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.assets.Guid
import kotlinx.serialization.Serializable

/**
 * What a meta file holds, the `.meta` file beside a source file: the asset's identity and nothing
 * that changes when the source is edited, so two people editing one source never conflict on it.
 */
@Doc("The contents of a meta file: the asset's GUID.")
@Serializable
public class AssetMeta(
  @Doc("The meta format version this file is written in.")
  public val formatVersion: Int = MetaFiles.FORMAT_VERSION,
  @Doc("The asset's identity; it stays the same when the source file is moved or renamed.")
  public val guid: Guid,
)
