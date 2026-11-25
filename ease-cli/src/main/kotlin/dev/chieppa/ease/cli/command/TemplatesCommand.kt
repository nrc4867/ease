package dev.chieppa.ease.cli.command

import dev.chieppa.ease.cli.Arguments
import dev.chieppa.ease.cli.CliCommand
import dev.chieppa.ease.cli.CliOutput
import dev.chieppa.ease.cli.ExitCodes
import dev.chieppa.ease.cli.template.TemplateSource

/** `ease templates`: one line for each project template and generator: kind, id, description. */
public class TemplatesCommand(private val templates: TemplateSource) : CliCommand {
  override val name: String = "templates"
  override val usage: String = ""
  override val summary: String = "List the project templates and generators"

  override fun run(arguments: Arguments, output: CliOutput): Int {
    arguments.requireOnly(emptySet())
    arguments.positional(0, "no arguments")
    templates.projects().forEach {
      output.out.println("project\t$it\t${templates.project(it).manifest.description}")
    }
    templates.generators().forEach {
      output.out.println("generator\t$it\t${templates.generator(it).manifest.description}")
    }
    return ExitCodes.OK
  }
}
