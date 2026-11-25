package dev.chieppa.ease.project.git

/** What one git command printed on [output] and [error], and how it ended. */
public class GitResult(
  public val exitCode: Int,
  public val output: String,
  public val error: String,
) {
  /** Whether the command ended with status 0. */
  public val succeeded: Boolean
    get() = exitCode == 0

  /** The fields of [output] written with `-z`, which ends each field with a zero byte. */
  public val nulFields: List<String>
    get() = output.split(NUL).filter { it.isNotEmpty() }

  /** The lines of [output], without empty ones. */
  public val lines: List<String>
    get() = output.lines().filter { it.isNotEmpty() }

  private companion object {
    const val NUL = '\u0000'
  }
}
