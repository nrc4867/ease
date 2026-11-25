// A game module: Kotlin on JDK 25 with explicit API, serialization and the Ease code generators,
// formatting and detekt held to the engine's rules, and the engine at the pinned version. The
// module that holds the game's GameModule applies ease.game-app, which adds the run tasks.
import dev.chieppa.ease.build.catalogVersion
import dev.chieppa.ease.build.configureDetektChecks
import dev.chieppa.ease.build.configureJunitTests
import dev.chieppa.ease.build.game.GameDependencies
import dev.chieppa.ease.build.game.WriteDetektConfig

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("com.google.devtools.ksp")
  id("dev.detekt")
  id("com.diffplug.spotless")
}

kotlin {
  jvmToolchain(catalogVersion("jvmToolchain").toInt())
  explicitApi()
}

dependencies {
  implementation(GameDependencies.module(project, "ease-runtime"))
  implementation(GameDependencies.module(project, "ease-annotations"))
  ksp(GameDependencies.module(project, "ease-codegen"))
  "testImplementation"(kotlin("test"))
}

spotless {
  lineEndings = com.diffplug.spotless.LineEnding.UNIX
  kotlin {
    targetExclude("build/**")
    ktfmt(catalogVersion("ktfmt")).googleStyle()
  }
  kotlinGradle { ktfmt(catalogVersion("ktfmt")).googleStyle() }
}

val detektConfig =
  tasks.register<WriteDetektConfig>("writeDetektConfig") {
    output = layout.buildDirectory.file("ease/detekt.yml")
  }

detekt {
  buildUponDefaultConfig = true
  config.setFrom(detektConfig.flatMap { it.output })
}

configureJunitTests()

configureDetektChecks()
