package dev.chieppa.ease.project.fix

/** What [ProjectFixer] changed, one sentence per change, in the order it made them. */
public class FixReport(
  /** The changes made. */
  public val changes: List<String>
) {
  /** Whether the fixer made no change. */
  public val isEmpty: Boolean
    get() = changes.isEmpty()
}
