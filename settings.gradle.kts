rootProject.name = "ease"

pluginManagement {
  includeBuild("build-logic")
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    // Compose Multiplatform's AndroidX dependencies, for the Stretch apps.
    google { mavenContent { includeGroupAndSubgroups("androidx") } }
    // wgpu-native's bindings, from GitHub Packages: credentials from GitHubPackagesUsername and
    // GitHubPackagesPassword (a token with read:packages in ~/.gradle/gradle.properties; CI passes
    // its own).
    maven {
      name = "GitHubPackages"
      url = uri("https://maven.pkg.github.com/nrc4867/webgpu-native-ffm")
      credentials(PasswordCredentials::class)
      mavenContent { includeModule("dev.chieppa", "webgpu-native-ffm") }
    }
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

// Stretch is built from source: the checkout named by stretch.dir (set it in
// ~/.gradle/gradle.properties), or a sibling folder named stretch.
val stretchDir =
  providers.gradleProperty("stretch.dir").orNull
    ?: file("../stretch").takeIf { it.isDirectory }?.path
    ?: error(
      "Stretch not found: set stretch.dir in ~/.gradle/gradle.properties to your Stretch checkout"
    )

includeBuild(stretchDir)

include(":ease-codegen")

include(":ease-testing")

include(":ease-math")

include(":ease-annotations")

include(":ease-ecs")

include(":ease-serialization")

include(":ease-commands")

include(":ease-platform")

include(":ease-assets")

include(":ease-scene")

include(":ease-net")

include(":ease-gpu")

include(":ease-render")

include(":ease-runtime")

include(":ease-player")

include(":ease-agent")

include(":sample")

include(":viewport-spike")

include(":architecture-tests")
