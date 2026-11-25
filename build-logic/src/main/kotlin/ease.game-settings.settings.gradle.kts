// A game's settings: Ease as a composite build from the checkout named by ease.dir (default
// ../ease) at the version the game pins, the engine's version catalog, and the repositories its
// dependencies come from.
import dev.chieppa.ease.build.game.GameDependencies

val easeDir =
  providers.gradleProperty(GameDependencies.DIR_PROPERTY).getOrElse(GameDependencies.DEFAULT_DIR)

GameDependencies.requirePinMatches(
  file(easeDir),
  providers.gradleProperty(GameDependencies.VERSION_PROPERTY).orNull,
)

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    // wgpu-native's bindings, from GitHub Packages: credentials from GitHubPackagesUsername and
    // GitHubPackagesPassword (a token with read:packages in ~/.gradle/gradle.properties).
    maven {
      name = "GitHubPackages"
      url = uri("https://maven.pkg.github.com/nrc4867/webgpu-native-ffm")
      credentials(PasswordCredentials::class)
      mavenContent { includeModule("dev.chieppa", "webgpu-native-ffm") }
    }
  }
  versionCatalogs { create("libs") { from(files("$easeDir/gradle/libs.versions.toml")) } }
}

includeBuild(easeDir)
