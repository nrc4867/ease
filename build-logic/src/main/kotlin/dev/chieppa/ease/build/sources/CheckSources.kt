package dev.chieppa.ease.build.sources

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Runs [sourceRules] over [sources]. Errors fail the task, naming each file and line; warnings are
 * printed. When [only] is set, as the pre-commit hook does with the staged files, only those files
 * are checked.
 */
@CacheableTask
abstract class CheckSources : DefaultTask() {
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val sources: ConfigurableFileCollection

  /** The repository root, which finding paths are relative to. */
  @get:Internal abstract val root: DirectoryProperty

  /** Repository-relative paths to restrict the check to; empty checks every source. */
  @get:Input abstract val only: ListProperty<String>

  /** Written when the check passes, so an unchanged set of sources is not read again. */
  @get:OutputFile abstract val report: RegularFileProperty

  @TaskAction
  fun run() {
    val rootDir = root.get().asFile
    val restrictTo = only.get().map { it.replace('\\', '/') }.toSet()
    val files =
      sources.files
        .map { file ->
          SourceFile(file.relativeTo(rootDir).invariantSeparatorsPath, file.readLines())
        }
        .filter { restrictTo.isEmpty() || it.path in restrictTo }
        .sortedBy { it.path }
    val findings = checkAll(files)
    findings.filter { it.severity == Severity.WARNING }.forEach { logger.warn("warning: $it") }
    val errors = findings.filter { it.severity == Severity.ERROR }
    if (errors.isNotEmpty()) {
      throw GradleException(
        "checkSources found ${errors.size} problem(s):\n" + errors.joinToString("\n")
      )
    }
    report.get().asFile.apply { parentFile.mkdirs() }.writeText("${files.size} files checked\n")
  }
}
