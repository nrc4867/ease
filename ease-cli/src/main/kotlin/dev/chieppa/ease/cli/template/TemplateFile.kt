package dev.chieppa.ease.cli.template

/** One file of a template: its path under the template's `files` folder, and its bytes. */
public class TemplateFile(
  /** The path, with `/` separators; it may hold `{{placeholders}}`. */
  public val path: String,
  /** The content, as stored. */
  public val bytes: ByteArray,
) {
  /** Whether the file is text with placeholders to fill in: its name ends in [SUFFIX]. */
  public val isText: Boolean
    get() = path.endsWith(SUFFIX)

  /** Where the file is written: [path] without the [SUFFIX]. */
  public val targetPath: String
    get() = path.removeSuffix(SUFFIX)

  /** What marks a file whose content has placeholders. */
  public companion object {
    /** The ending of a file whose content has placeholders; every other file is copied as is. */
    public const val SUFFIX: String = ".tpl"
  }
}
