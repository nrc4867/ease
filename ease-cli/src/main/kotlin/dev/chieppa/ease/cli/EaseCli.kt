package dev.chieppa.ease.cli

import dev.chieppa.ease.cli.command.CheckCommand
import dev.chieppa.ease.cli.command.GenerateCommand
import dev.chieppa.ease.cli.command.ImportCommand
import dev.chieppa.ease.cli.command.MergeDriverCommand
import dev.chieppa.ease.cli.command.NewCommand
import dev.chieppa.ease.cli.command.TemplatesCommand
import dev.chieppa.ease.cli.hook.GitInitHook
import dev.chieppa.ease.cli.hook.PostGenerateHook
import dev.chieppa.ease.cli.template.TemplateSource

/** The `ease` command line: picks the command the first argument names and runs it. */
public class EaseCli(
  private val commands: List<CliCommand>,
  private val output: CliOutput = CliOutput(System.out, System.err),
) {
  /** Runs `ease` with [args] and returns its exit status. */
  public fun run(args: List<String>): Int {
    val command = args.firstOrNull()?.let { word -> commands.find { it.name == word } }
    if (command == null) {
      printUsage(args.firstOrNull())
      return ExitCodes.USAGE
    }
    return try {
      command.run(Arguments.parse(args.drop(1)), output)
    } catch (failure: CliFailure) {
      output.err.println("ease ${command.name}: ${failure.message}")
      output.err.println("usage: ease ${command.name} ${command.usage}")
      ExitCodes.FAILED
    }
  }

  private fun printUsage(unknown: String?) {
    if (unknown != null) output.err.println("ease: unknown command '$unknown'")
    output.err.println("usage: ease <command> [arguments]")
    commands.forEach { output.err.println("  ${it.name.padEnd(NAME_WIDTH)}${it.summary}") }
  }

  /** The commands `ease` ships. */
  public companion object {
    private const val NAME_WIDTH = 12

    /**
     * The command line with its commands: templates from [templates] (the ones shipped in the
     * distribution by default), and [hooks] run after a project is generated (by default, putting
     * it in git).
     */
    public fun standard(
      templates: TemplateSource = TemplateSource.shipped(),
      output: CliOutput = CliOutput(System.out, System.err),
      hooks: List<PostGenerateHook> = listOf(GitInitHook(output, EaseLauncher.command())),
    ): EaseCli =
      EaseCli(
        listOf(
          NewCommand(templates, hooks),
          GenerateCommand(templates),
          TemplatesCommand(templates),
          CheckCommand(),
          ImportCommand(),
          MergeDriverCommand(),
        ),
        output,
      )
  }
}
