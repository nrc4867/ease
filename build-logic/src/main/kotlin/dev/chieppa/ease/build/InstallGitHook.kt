package dev.chieppa.ease.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Copies [script] to the repository's `pre-commit` hook, [hook]. The hook runs `spotlessCheck` and
 * `checkSources` on the staged files only.
 */
abstract class InstallGitHook : DefaultTask() {
  @get:InputFile @get:PathSensitive(PathSensitivity.NONE) abstract val script: RegularFileProperty

  @get:OutputFile abstract val hook: RegularFileProperty

  @TaskAction
  fun install() {
    val target = hook.get().asFile
    target.parentFile.mkdirs()
    script.get().asFile.copyTo(target, overwrite = true)
    target.setExecutable(true)
  }
}
