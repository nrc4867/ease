package dev.chieppa.ease.assets.database

import dev.chieppa.ease.assets.Guid
import java.nio.file.Path

/** What happened to one asset between two syncs. */
public sealed interface AssetChange {
  /** The asset concerned. */
  public val guid: Guid

  /** A new asset appeared. */
  public class Added(public val record: AssetRecord) : AssetChange {
    override val guid: Guid
      get() = record.guid
  }

  /** The source file's contents changed; [record] holds the new hash. */
  public class Changed(public val record: AssetRecord) : AssetChange {
    override val guid: Guid
      get() = record.guid
  }

  /** The source file moved from [from] to the path in [record], keeping its GUID. */
  public class Moved(public val record: AssetRecord, public val from: Path) : AssetChange {
    override val guid: Guid
      get() = record.guid
  }

  /** The asset is gone: its source file or its meta file was deleted. */
  public class Removed(override val guid: Guid, public val path: Path) : AssetChange
}
