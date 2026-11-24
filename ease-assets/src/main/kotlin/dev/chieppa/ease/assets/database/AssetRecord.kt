package dev.chieppa.ease.assets.database

import dev.chieppa.ease.assets.Guid
import java.nio.file.Path

/** One asset the database knows: its identity, where its source is, and that source's hash. */
public class AssetRecord(
  /** The asset's identity. */
  public val guid: Guid,
  /** The source file's path relative to the project folder. */
  public val path: Path,
  /** The source file's fingerprint as the database last read it (see `ContentHash`). */
  public val contentHash: String,
) {
  override fun toString(): String = "$path ($guid)"
}
