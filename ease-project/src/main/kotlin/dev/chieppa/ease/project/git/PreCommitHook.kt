package dev.chieppa.ease.project.git

/** The pre-commit hook of a project: `ease check` over the whole project before every commit. */
public object PreCommitHook {
  /** The hook's path inside the repository's `.git` folder. */
  public const val PATH: String = "hooks/pre-commit"

  /** The hook script, running the `ease` program named by [easeCommand]. */
  public fun script(easeCommand: String): String =
    listOf(
        "#!/bin/sh",
        "# Written by ease new: stops a commit while ease check finds an error.",
        "ease_bin=${ShellQuote.quote(easeCommand)}",
        "cd \"\$(git rev-parse --show-toplevel)\" || exit 1",
        "if [ ! -x \"\$ease_bin\" ] && ! command -v \"\$ease_bin\" >/dev/null 2>&1; then",
        "  echo \"pre-commit: ease is not at \$ease_bin; put it on PATH or edit this hook\" >&2",
        "  exit 1",
        "fi",
        "exec \"\$ease_bin\" check",
      )
      .joinToString("\n", postfix = "\n")
}
