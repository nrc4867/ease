package dev.chieppa.ease.cli.command

import dev.chieppa.ease.cli.Arguments
import dev.chieppa.ease.cli.CliCommand
import dev.chieppa.ease.cli.CliFailure
import dev.chieppa.ease.cli.CliOutput
import dev.chieppa.ease.cli.ExitCodes
import dev.chieppa.ease.project.git.Git
import dev.chieppa.ease.project.git.GitFailure
import dev.chieppa.ease.project.merge.MergeDriver
import java.nio.file.Path

/**
 * `ease merge-driver <ancestor> <current> <other> <path>`: the command git runs for a file
 * `.gitattributes` gives `merge=ease` (the arguments are git's `%O %A %B %P`). It writes the merge
 * into `<current>` and ends with status 0, or leaves conflict markers there and ends with status 1.
 */
public class MergeDriverCommand(private val workingDirectory: Path = Path.of("").toAbsolutePath()) :
  CliCommand {
  override val name: String = "merge-driver"
  override val usage: String = "<ancestor> <current> <other> <path>"
  override val summary: String = "Merge scene and node files for git (run by git, not by hand)"

  override fun run(arguments: Arguments, output: CliOutput): Int {
    val (ancestor, current, other) = arguments.positional(PARTS, usage).take(FILES).map(::file)
    return try {
      if (MergeDriver(Git(workingDirectory)).merge(ancestor, current, other)) ExitCodes.OK
      else ExitCodes.FAILED
    } catch (failure: GitFailure) {
      throw CliFailure(failure.message.orEmpty(), failure)
    }
  }

  private fun file(name: String): Path = workingDirectory.resolve(name).normalize()

  private companion object {
    const val FILES = 3
    const val PARTS = 4
  }
}
