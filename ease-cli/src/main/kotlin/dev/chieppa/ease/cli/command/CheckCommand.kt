package dev.chieppa.ease.cli.command

import dev.chieppa.ease.cli.Arguments
import dev.chieppa.ease.cli.CliCommand
import dev.chieppa.ease.cli.CliFailure
import dev.chieppa.ease.cli.CliOutput
import dev.chieppa.ease.cli.ExitCodes
import dev.chieppa.ease.cli.Options
import dev.chieppa.ease.project.check.ProjectChecker
import dev.chieppa.ease.project.fix.ProjectFixer
import dev.chieppa.ease.project.git.GitFailure
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * `ease check [--dir <project>] [--fix]`: the rules that catch a bad merge (see [ProjectChecker]).
 * It prints one line per finding, `<severity> <kind> <file>: <message>`, then a count, and ends
 * with status 1 when there is an error. `--fix` first makes the mechanical fixes (see
 * [ProjectFixer]), says what it changed, and reports what is still wrong.
 */
public class CheckCommand(private val workingDirectory: Path = Path.of("").toAbsolutePath()) :
  CliCommand {
  override val name: String = "check"
  override val usage: String = "[--dir <project>] [--fix]"
  override val summary: String =
    "Check a project for the damage a merge can do, and fix what is safe"

  override fun run(arguments: Arguments, output: CliOutput): Int {
    arguments.requireOnly(setOf(Options.DIR, Options.FIX))
    arguments.positional(0, "no arguments")
    val project = workingDirectory.resolve(arguments.option(Options.DIR) ?: ".").normalize()
    if (!Files.isDirectory(project)) throw CliFailure("$project is not a folder")
    return try {
      if (arguments.flag(Options.FIX)) fix(project, output)
      val report = ProjectChecker(project).check()
      report.findings.forEach { output.out.println(it) }
      output.out.println("ease check: ${report.summary()}")
      if (report.passed) ExitCodes.OK else ExitCodes.FAILED
    } catch (failure: GitFailure) {
      throw CliFailure(failure.message.orEmpty(), failure)
    } catch (failure: IOException) {
      throw CliFailure("${failure.javaClass.simpleName}: ${failure.message}", failure)
    }
  }

  private fun fix(project: Path, output: CliOutput) {
    val report = ProjectFixer(project).fix()
    if (report.isEmpty) output.out.println("ease check --fix: nothing to fix")
    report.changes.forEach { output.out.println("fixed  $it") }
  }
}
