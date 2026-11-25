package dev.chieppa.ease.project.check

/** One thing wrong in a project: how much it matters, what it is, which file and in words. */
public class Finding(
  /** How much it matters. */
  public val severity: Severity,
  /** What it is about. */
  public val kind: FindingKind,
  /** The file it is in, relative to the project and written with `/`. */
  public val file: String,
  /** A sentence saying what is wrong and how to put it right. */
  public val message: String,
) {
  /** The finding as one report line. */
  override fun toString(): String =
    "${severity.name.lowercase().padEnd(SEVERITY_WIDTH)}${kind.id.padEnd(KIND_WIDTH)}$file: $message"

  private companion object {
    const val SEVERITY_WIDTH = 8
    const val KIND_WIDTH = 22
  }
}
