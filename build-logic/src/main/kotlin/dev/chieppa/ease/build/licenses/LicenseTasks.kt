package dev.chieppa.ease.build.licenses

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** What every license task reads: the shipped modules and the overrides file. */
abstract class LicenseTask : DefaultTask() {
  @get:Input abstract val modules: ListProperty<ModuleLicenses>

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val overrides: RegularFileProperty

  protected fun overrideList(): List<LicenseOverride> =
    LicenseOverrides.parse(overrides.get().asFile.readLines())

  protected fun shipped(): List<ShippedLicense> =
    LicenseCheck.resolve(modules.get(), overrideList())
}

/** Fails when a shipped module's license may not ship, or an override names no shipped module. */
@CacheableTask
abstract class CheckLicenses : LicenseTask() {
  /** Written when the check passes: each shipped module and its license. */
  @get:OutputFile abstract val report: RegularFileProperty

  @TaskAction
  fun run() {
    val shipped = shipped()
    val problems =
      shipped.mapNotNull { it.problem?.let { problem -> "${it.module}:${it.version} $problem" } } +
        LicenseCheck.unusedOverrides(modules.get(), overrideList())
    if (problems.isNotEmpty()) {
      throw GradleException(
        "checkLicenses found ${problems.size} problem(s):\n" + problems.joinToString("\n")
      )
    }
    report.get().asFile.apply { parentFile.mkdirs() }.writeText(Notice.render("", shipped))
  }
}

/** Writes [notice] from [header] and the shipped modules. */
abstract class GenerateNotice : LicenseTask() {
  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val header: RegularFileProperty

  @get:OutputFile abstract val notice: RegularFileProperty

  @TaskAction
  fun run() {
    notice.get().asFile.writeText(Notice.render(header.get().asFile.readText(), shipped()))
  }
}

/** Fails when [notice] differs from what `generateNotice` would write. */
@CacheableTask
abstract class CheckNotice : LicenseTask() {
  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val header: RegularFileProperty

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val notice: RegularFileProperty

  /** Written when the check passes, so an unchanged notice is not read again. */
  @get:OutputFile abstract val report: RegularFileProperty

  @TaskAction
  fun run() {
    val expected = Notice.render(header.get().asFile.readText(), shipped())
    if (!Notice.isCurrent(notice.get().asFile.readText(), expected)) {
      throw GradleException("NOTICE is stale: run ./gradlew generateNotice and commit it")
    }
    report.get().asFile.apply { parentFile.mkdirs() }.writeText("NOTICE is current\n")
  }
}
