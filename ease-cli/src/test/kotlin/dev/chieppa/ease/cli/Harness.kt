package dev.chieppa.ease.cli

import dev.chieppa.ease.cli.command.CheckCommand
import dev.chieppa.ease.cli.command.GenerateCommand
import dev.chieppa.ease.cli.command.MergeDriverCommand
import dev.chieppa.ease.cli.command.NewCommand
import dev.chieppa.ease.cli.command.TemplatesCommand
import dev.chieppa.ease.cli.hook.PostGenerateHook
import dev.chieppa.ease.cli.template.TemplateSource
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Path

/** The command line run against the shipped templates, with its output captured. */
class Harness(
  workingDirectory: Path,
  hooks: List<PostGenerateHook> = emptyList(),
  templates: TemplateSource = TemplateSource.shipped(),
) {
  private val out = ByteArrayOutputStream()
  private val err = ByteArrayOutputStream()
  private val cli =
    EaseCli(
      listOf(
        NewCommand(templates, hooks, workingDirectory),
        GenerateCommand(templates, workingDirectory),
        TemplatesCommand(templates),
        CheckCommand(workingDirectory),
        MergeDriverCommand(workingDirectory),
      ),
      CliOutput(PrintStream(out, true, "UTF-8"), PrintStream(err, true, "UTF-8")),
    )

  /** Runs `ease` with [words] and returns its exit status. */
  fun run(vararg words: String): Int = cli.run(words.toList())

  val stdout: String
    get() = out.toString("UTF-8")

  val stderr: String
    get() = err.toString("UTF-8")
}
