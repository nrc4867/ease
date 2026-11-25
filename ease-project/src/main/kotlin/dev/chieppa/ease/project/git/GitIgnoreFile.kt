package dev.chieppa.ease.project.git

/** The `.gitignore` a new project starts with. */
public object GitIgnoreFile {
  /** The file's name. */
  public const val NAME: String = ".gitignore"

  /**
   * The `.gitignore` text: build output, IDE files, the import cache, cooked output, logs and
   * per-user editor state.
   */
  public fun text(): String = buildString {
    appendLine("# Build output")
    appendLine("build/")
    appendLine(".gradle/")
    appendLine(".kotlin/")
    appendLine("out/")
    appendLine()
    appendLine("# IDE files")
    appendLine(".idea/")
    appendLine("*.iml")
    appendLine("local.properties")
    appendLine()
    appendLine(
      "# Ease: import cache, cooked output, logs and per-user editor state (layout, recent"
    )
    appendLine("# files, camera positions)")
    appendLine(".ease/cache/")
    appendLine(".ease/cooked/")
    appendLine(".ease/logs/")
    appendLine(".ease/user/")
    appendLine("*.log")
  }
}
