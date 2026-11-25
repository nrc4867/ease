package dev.chieppa.ease.cli

/** One `ease` command, such as `new`. */
public interface CliCommand {
  /** The word that selects this command. */
  public val name: String

  /** One line naming the arguments, as `ease` prints it. */
  public val usage: String

  /** What the command does, in one line. */
  public val summary: String

  /** Runs the command on the words after its name. Throws [CliFailure] for a fixable mistake. */
  public fun run(arguments: Arguments, output: CliOutput): Int
}
