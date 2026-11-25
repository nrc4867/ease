package dev.chieppa.ease.cli.command

import dev.chieppa.ease.cli.Arguments
import dev.chieppa.ease.cli.CliCommand
import dev.chieppa.ease.cli.CliFailure
import dev.chieppa.ease.cli.CliOutput
import dev.chieppa.ease.cli.EaseVersion
import dev.chieppa.ease.cli.ExitCodes
import dev.chieppa.ease.cli.Options
import dev.chieppa.ease.cli.hook.GeneratedProject
import dev.chieppa.ease.cli.hook.PostGenerateHook
import dev.chieppa.ease.cli.project.ProjectFile
import dev.chieppa.ease.cli.template.EaseCheckout
import dev.chieppa.ease.cli.template.GenerationPlan
import dev.chieppa.ease.cli.template.Naming
import dev.chieppa.ease.cli.template.Placeholders
import dev.chieppa.ease.cli.template.ReservedNames
import dev.chieppa.ease.cli.template.TemplateSource
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

/**
 * `ease new <template> --name <name>`: a game from a project template, in a folder of the name's
 * kebab form under [workingDirectory] unless `--dir` says otherwise, then [hooks] run on it.
 */
public class NewCommand(
  private val templates: TemplateSource,
  private val hooks: List<PostGenerateHook> = emptyList(),
  private val workingDirectory: Path = Path.of("").toAbsolutePath(),
) : CliCommand {
  override val name: String = "new"
  override val usage: String =
    "<template> --name <name> [--package <package>] [--dir <folder>] [--ease-version <version>] " +
      "[--no-git]"
  override val summary: String = "Create a game from a project template"

  override fun run(arguments: Arguments, output: CliOutput): Int {
    arguments.requireOnly(OPTIONS)
    val id = arguments.positional(1, "a template id").single()
    val template = templates.project(id)
    val project = projectFile(arguments, id, template.manifest.gameModule)
    val directory = target(arguments.option(Options.DIR), project)
    GenerationPlan.of(template, Placeholders(project.placeholders()), directory).apply()
    ProjectFile.write(directory, project)
    val versionControl = !arguments.flag(Options.NO_GIT)
    hooks.forEach { it.run(GeneratedProject(directory, project, versionControl)) }
    output.out.println("Created ${project.name} in $directory from the $id template")
    return ExitCodes.OK
  }

  private fun projectFile(arguments: Arguments, template: String, module: String): ProjectFile {
    val name =
      Naming.requireName(arguments.option(Options.NAME) ?: throw CliFailure("--name is required"))
    val defaultPackage = Naming.defaultPackage(DEFAULT_PACKAGE_ROOT, name)
    val project =
      ProjectFile(
        name = name,
        packageName = Naming.requirePackage(arguments.option(Options.PACKAGE) ?: defaultPackage),
        className = Naming.pascal(name),
        module = module,
        easeVersion = arguments.option(Options.EASE_VERSION) ?: EaseVersion.current,
        template = template,
      )
    ReservedNames.requireFree(project.className)
    return project.validated()
  }

  /** The folder to generate in: [explicit] if given, else the name's own folder. */
  private fun target(explicit: String?, project: ProjectFile): Path {
    val directory = workingDirectory.resolve(explicit ?: Naming.kebab(project.name)).normalize()
    val checkout = EaseCheckout.containing(directory)
    if (explicit == null && checkout != null) {
      throw CliFailure(
        "$directory is inside the Ease checkout $checkout: " +
          "give --dir to say where the game goes, outside it"
      )
    }
    if (directory.exists() && (!directory.isDirectory() || isNotEmpty(directory))) {
      throw CliFailure("$directory already exists and is not empty")
    }
    return directory
  }

  private fun isNotEmpty(directory: Path): Boolean =
    Files.list(directory).use { it.findAny().isPresent }

  private companion object {
    const val DEFAULT_PACKAGE_ROOT = "example"
    val OPTIONS =
      setOf(Options.NAME, Options.PACKAGE, Options.DIR, Options.EASE_VERSION, Options.NO_GIT)
  }
}
