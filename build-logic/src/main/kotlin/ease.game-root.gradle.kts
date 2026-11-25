// A game's root project: checkSources over the whole repository and formatting for the files
// outside any module.
import dev.chieppa.ease.build.catalogVersion
import dev.chieppa.ease.build.sources.CheckSources

plugins {
  base
  id("com.diffplug.spotless")
}

spotless {
  lineEndings = com.diffplug.spotless.LineEnding.UNIX
  kotlinGradle {
    target("*.gradle.kts", "*/*.gradle.kts")
    targetExclude("**/build/**")
    ktfmt(catalogVersion("ktfmt")).googleStyle()
  }
}

val checkSources =
  tasks.register<CheckSources>("checkSources") {
    group = "verification"
    description = "Checks every source and Markdown file against the rules no linter covers."
    sources.from(
      fileTree(rootDir) {
        include("**/*.kt", "**/*.kts", "**/*.java", "**/*.md")
        exclude("**/build/**", "**/.gradle/**", "**/.kotlin/**")
      }
    )
    root = layout.projectDirectory
    only =
      providers
        .gradleProperty("stagedFiles")
        .map { it.split(',').filter(String::isNotBlank) }
        .orElse(emptyList())
    report = layout.buildDirectory.file("checkSources.txt")
  }

tasks.named("check") { dependsOn(checkSources) }
