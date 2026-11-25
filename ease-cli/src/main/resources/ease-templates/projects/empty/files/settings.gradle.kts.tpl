// Ease comes in as a composite build from the checkout named by ease.dir (see gradle.properties).
pluginManagement {
  val easeDir = providers.gradleProperty("ease.dir").getOrElse("../ease")
  includeBuild("$easeDir/build-logic")
}

plugins { id("ease.game-settings") }

rootProject.name = "{{projectId}}"

// ease:modules
include(":game")
