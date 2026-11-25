package dev.chieppa.ease.assets.importing

import java.nio.file.Path

/** Something that kept a source from importing, shown on the file. */
public class ImportProblem(
  /** The source file, relative to the project's content folder. */
  public val source: Path,
  /** What is wrong, in a sentence. */
  public val message: String,
) {
  override fun toString(): String = "$source: $message"
}
