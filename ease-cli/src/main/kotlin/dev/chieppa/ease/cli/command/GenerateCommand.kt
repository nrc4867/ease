package dev.chieppa.ease.cli.command

import dev.chieppa.ease.cli.Arguments
import dev.chieppa.ease.cli.CliCommand
import dev.chieppa.ease.cli.CliOutput
import dev.chieppa.ease.cli.ExitCodes
import dev.chieppa.ease.cli.Options
import dev.chieppa.ease.cli.project.ProjectFile
import dev.chieppa.ease.cli.template.GenerationPlan
import dev.chieppa.ease.cli.template.Naming
import dev.chieppa.ease.cli.template.Placeholders
import dev.chieppa.ease.cli.template.ReservedNames
import dev.chieppa.ease.cli.template.TemplateSource
import java.nio.file.Path

/**
 * `ease generate <generator> <name>`: adds a module, a component or a system to the game project in
 * [workingDirectory] (or `--dir`), by the same rules `ease new` follows. Nothing is written unless
 * the whole generator can be.
 */
public class GenerateCommand(
  private val templates: TemplateSource,
  private val workingDirectory: Path = Path.of("").toAbsolutePath(),
) : CliCommand {
  override val name: String = "generate"
  override val usage: String = "<generator> <name> [--dir <project folder>]"
  override val summary: String = "Add a module, component or system to a game project"

  override fun run(arguments: Arguments, output: CliOutput): Int {
    arguments.requireOnly(setOf(Options.DIR))
    val (id, rawName) = arguments.positional(2, "a generator and a name")
    val generator = templates.generator(id)
    val typeName = Naming.requireName(rawName)
    ReservedNames.requireFree(Naming.pascal(typeName), SYSTEM_SUFFIX)
    val directory = workingDirectory.resolve(arguments.option(Options.DIR) ?: ".").normalize()
    val project = ProjectFile.read(directory)
    val placeholders =
      Placeholders(project.placeholders()) +
        mapOf(
          "name" to typeName,
          "Name" to Naming.pascal(typeName),
          "nameId" to Naming.kebab(typeName),
          "nameWords" to Naming.spaced(typeName),
        )
    val plan = GenerationPlan.of(generator, placeholders, directory)
    plan.apply()
    plan.created.forEach { output.out.println("created ${directory.relativize(it)}") }
    plan.changed.forEach { output.out.println("changed ${directory.relativize(it)}") }
    return ExitCodes.OK
  }

  private companion object {
    /** What the system generator adds to a name, so its type clashes are checked too. */
    const val SYSTEM_SUFFIX = "System"
  }
}
