package dev.chieppa.ease.cli.hook

import dev.chieppa.ease.cli.CliFailure
import dev.chieppa.ease.cli.CliOutput
import dev.chieppa.ease.project.git.Git
import dev.chieppa.ease.project.git.GitFailure
import dev.chieppa.ease.project.git.GitSetup
import java.nio.file.Path

/**
 * Puts a new project in git (see [GitSetup]) unless `--no-git` was given: `git init`, `.gitignore`,
 * `.gitattributes`, Git LFS, the merge driver, the first commit and the pre-commit hook that runs
 * `ease check`. [easeCommand] is the program those run; [git] says how git is run in a folder.
 */
public class GitInitHook(
  private val output: CliOutput,
  private val easeCommand: String,
  private val git: (Path) -> Git = { Git(it) },
) : PostGenerateHook {
  override fun run(project: GeneratedProject) {
    if (!project.versionControl) return
    val result =
      try {
        GitSetup(git(project.directory), easeCommand)
          .apply(project.directory, "Create ${project.file.name}")
      } catch (failure: GitFailure) {
        throw CliFailure("${failure.message} (${project.directory} was written)", failure)
      }
    result.notes.forEach { output.err.println("ease new: $it") }
  }
}
