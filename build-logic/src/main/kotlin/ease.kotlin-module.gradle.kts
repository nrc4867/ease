// A Kotlin module: JDK 25, ktfmt in Google style and detekt with the repository's config.
import dev.chieppa.ease.build.catalogVersion
import dev.chieppa.ease.build.configureDetektChecks

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("dev.detekt")
  id("ease.module")
}

kotlin {
  jvmToolchain(catalogVersion("jvmToolchain").toInt())
  explicitApi()
}

// Code KSP writes under build/ is neither formatted nor analyzed: it is checked through the
// processor that writes it.
spotless {
  kotlin {
    targetExclude("build/**")
    ktfmt(catalogVersion("ktfmt")).googleStyle()
  }
  kotlinGradle { ktfmt(catalogVersion("ktfmt")).googleStyle() }
}

detekt {
  buildUponDefaultConfig = true
  config.setFrom(rootDir.resolve("config/detekt/detekt.yml"))
}

dependencies { "testImplementation"(kotlin("test")) }

configureDetektChecks()
