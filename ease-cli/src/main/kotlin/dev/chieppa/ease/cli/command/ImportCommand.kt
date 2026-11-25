package dev.chieppa.ease.cli.command

import dev.chieppa.ease.assets.associations.ContributorProvider
import dev.chieppa.ease.assets.associations.FileAssociationRegistry
import dev.chieppa.ease.assets.associations.ModuleRegistrations
import dev.chieppa.ease.assets.database.AssetDatabase
import dev.chieppa.ease.assets.database.AssetProblem
import dev.chieppa.ease.assets.importing.ImportService
import dev.chieppa.ease.cli.Arguments
import dev.chieppa.ease.cli.CliCommand
import dev.chieppa.ease.cli.CliFailure
import dev.chieppa.ease.cli.CliOutput
import dev.chieppa.ease.cli.ExitCodes
import dev.chieppa.ease.cli.Options
import dev.chieppa.ease.platform.files.ManualFileWatcher
import dev.chieppa.ease.project.layout.ProjectLayout
import dev.chieppa.ease.scene.assets.registerScenes
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * `ease import [--dir <project>] [--adopt]`: runs every importer over the project's content,
 * headless. The importers are Ease's own and those of every `ContributorProvider` on the classpath;
 * results go to this machine's cache and only sources whose inputs changed run again. It prints one
 * line per import and per problem, then a count, and ends with status 1 when anything is wrong: a
 * source that fails to import, an ambiguous or unknown importer, or a file the asset database
 * rejects. `--adopt` first gives every source without a meta file one.
 */
public class ImportCommand(private val workingDirectory: Path = Path.of("").toAbsolutePath()) :
  CliCommand {
  override val name: String = "import"
  override val usage: String = "[--dir <project>] [--adopt]"
  override val summary: String = "Run every importer over a project's content"

  override fun run(arguments: Arguments, output: CliOutput): Int {
    arguments.requireOnly(setOf(Options.DIR, Options.ADOPT))
    arguments.positional(0, "no arguments")
    val project = workingDirectory.resolve(arguments.option(Options.DIR) ?: ".").normalize()
    val content = ProjectLayout.content(project)
    if (!Files.isDirectory(content)) throw CliFailure("$content is not a folder")
    return try {
      importAll(content, arguments.flag(Options.ADOPT), output)
    } catch (failure: IOException) {
      throw CliFailure("${failure.javaClass.simpleName}: ${failure.message}", failure)
    }
  }

  private fun importAll(content: Path, adopt: Boolean, output: CliOutput): Int {
    val associations = FileAssociationRegistry()
    associations.registerScenes()
    val modules = ModuleRegistrations(associations) // one scope for every game module
    ContributorProvider.loadAll().forEach { it.registerContributors(modules) }
    val database = AssetDatabase.open(content, ManualFileWatcher(), associations = associations)
    if (adopt) adopt(database, output)
    val sourceProblems = database.problems
    sourceProblems.forEach { output.out.println("error   ${it.message}") }
    val report = ImportService(database, associations).use { it.refresh() }
    report.imported.forEach { output.out.println("imported ${it.joinToString("/")}") }
    report.problems.forEach { output.out.println("error   $it") }
    val count = sourceProblems.size + report.problems.size
    output.out.println(
      "ease import: ${report.imported.size} imported, ${report.upToDate} up to date, " +
        "$count ${if (count == 1) "problem" else "problems"}"
    )
    return if (count == 0) ExitCodes.OK else ExitCodes.FAILED
  }

  private fun adopt(database: AssetDatabase, output: CliOutput) {
    for (problem in database.problems.filterIsInstance<AssetProblem.SourceWithoutMeta>()) {
      database.adopt(problem.source)
      output.out.println("adopted ${problem.source.joinToString("/")}")
    }
  }
}
