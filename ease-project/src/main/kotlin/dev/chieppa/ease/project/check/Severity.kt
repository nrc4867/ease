package dev.chieppa.ease.project.check

/** How much a [Finding] matters. */
public enum class Severity {
  /** Something wrong that must be fixed before committing; `ease check` ends with status 1. */
  ERROR,

  /** Something to look at that does not stop a commit. */
  WARNING,
}
