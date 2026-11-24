package dev.chieppa.ease.assets.database

import dev.chieppa.ease.assets.Guid
import java.nio.file.Path

/** Something wrong with the project's files that the database refuses to hide. */
public sealed interface AssetProblem {
  /** A sentence naming the files involved. */
  public val message: String

  /** The same GUID in several meta files, usually from a meta file copied by hand. */
  public class DuplicateGuid(public val guid: Guid, public val sources: List<Path>) : AssetProblem {
    override val message: String
      get() = "GUID $guid is used by ${sources.joinToString()}"
  }

  /** A meta file whose source is gone and that no new file matches by content. */
  public class MetaWithoutSource(public val meta: Path) : AssetProblem {
    override val message: String
      get() = "Meta file $meta has no source file"
  }

  /** A source file nobody gave a meta file; importing it creates one. */
  public class SourceWithoutMeta(public val source: Path) : AssetProblem {
    override val message: String
      get() = "Source file $source has no meta file"
  }

  /** A meta file that cannot be read. */
  public class UnreadableMeta(public val meta: Path, public val reason: String) : AssetProblem {
    override val message: String
      get() = "Meta file $meta cannot be read: $reason"
  }
}
