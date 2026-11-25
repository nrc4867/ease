package dev.chieppa.ease.project.git

/** Quoting for the POSIX shell git runs hooks and merge drivers with. */
public object ShellQuote {
  /** [text] in single quotes, safe for any characters inside. */
  public fun quote(text: String): String = "'" + text.replace("'", "'\\''") + "'"
}
