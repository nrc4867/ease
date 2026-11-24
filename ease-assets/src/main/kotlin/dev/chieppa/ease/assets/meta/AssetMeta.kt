package dev.chieppa.ease.assets.meta

import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.assets.Guid
import kotlinx.serialization.Serializable

/**
 * What a meta file holds, the `.meta` file beside a source file: the asset's identity and the
 * fingerprint of the source it was last seen with.
 */
@Doc("The contents of a meta file: the asset's GUID and the hash of its source file.")
@Serializable
public class AssetMeta(
  @Doc("The meta format version this file is written in.")
  public val formatVersion: Int = MetaFiles.FORMAT_VERSION,
  @Doc("The asset's identity; it stays the same when the source file is moved or renamed.")
  public val guid: Guid,
  @Doc(
    "The source file's fingerprint: `sha256:` and the hash as lowercase hex. A moved file " +
      "finds its meta file by it."
  )
  public val contentHash: String,
)
