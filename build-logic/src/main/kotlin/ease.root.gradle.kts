// The root project: checkSources over the whole repository, the pre-commit hook, and formatting
// for the files outside any module.
import dev.chieppa.ease.build.InstallGitHook
import dev.chieppa.ease.build.catalogVersion
import dev.chieppa.ease.build.registerCheckSources

plugins {
  id("ease.licenses")
  base
  id("com.diffplug.spotless")
}

spotless {
  kotlinGradle {
    target("*.gradle.kts", "build-logic/**/*.kts")
    targetExclude("**/build/**")
    ktfmt(catalogVersion("ktfmt")).googleStyle()
  }
  kotlin {
    target("build-logic/src/**/*.kt")
    ktfmt(catalogVersion("ktfmt")).googleStyle()
  }
}

// The build logic's own tests hold examples of what the rules catch, so the rules skip them.
registerCheckSources(listOf("build-logic/src/test/**"))

tasks.named("check") { dependsOn(gradle.includedBuild("build-logic").task(":test")) }

// The hooks folder git uses for this checkout, which a worktree shares with its main checkout.
val hooksDir =
  providers
    .exec {
      commandLine("git", "rev-parse", "--path-format=absolute", "--git-path", "hooks")
      isIgnoreExitValue = true
    }
    .standardOutput
    .asText
    .map { it.trim() }

// Outside a git checkout, such as an unpacked source archive, the hook goes nowhere it is read.
tasks.register<InstallGitHook>("installGitHook") {
  group = "build setup"
  description = "Installs the pre-commit hook that runs spotlessCheck and checkSources."
  script = layout.projectDirectory.file("tools/pre-commit")
  hook =
    layout.projectDirectory.file(
      hooksDir.map { if (it.isEmpty()) "build/no-git/pre-commit" else "$it/pre-commit" }
    )
}
