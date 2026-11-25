// A game's root project: checkSources over the whole repository and formatting for the files
// outside any module.
import dev.chieppa.ease.build.catalogVersion
import dev.chieppa.ease.build.registerCheckSources

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

registerCheckSources()
