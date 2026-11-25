package dev.chieppa.ease.build

import org.gradle.api.provider.ProviderFactory

/**
 * jolt-jni's jars. jolt-jni publishes one artifact per platform: its main jar holds the Java
 * classes, the same on every platform, and its `ReleaseSp` jar the single-precision release
 * library. Every build takes the classes from one artifact, [CLASSES], and the library from the
 * artifact of the platform [NativePlatform] picks.
 */
object JoltNatives {
  /** The classifier of the natives jar: the release build in single precision. */
  const val CLASSIFIER = "ReleaseSp"

  private const val GROUP = "com.github.stephengold"

  /** The artifact the classes come from on every platform. */
  const val CLASSES = "jolt-jni-Linux64"

  /** The `group:artifact:version` of jolt-jni's classes, at [version]. */
  fun classes(version: String): String = "$GROUP:$CLASSES:$version"

  /** The `group:artifact:version:classifier` of the platform's natives jar, at [version]. */
  fun natives(providers: ProviderFactory, version: String): String =
    "$GROUP:${artifact(NativePlatform.of(providers))}:$version:$CLASSIFIER"

  /**
   * [coordinates] (`group:artifact:version`) with any platform's jolt-jni artifact named as
   * [CLASSES], so the list of shipped libraries is the same whichever platform builds it.
   */
  fun portable(coordinates: String): String {
    val (group, artifact, version) = coordinates.split(":")
    val platformArtifact = group == GROUP && artifact.startsWith("jolt-jni-")
    return if (platformArtifact) "$GROUP:$CLASSES:$version" else coordinates
  }

  /** The artifact jolt-jni publishes for [platform], such as `jolt-jni-Windows64`. */
  fun artifact(platform: String): String =
    when (platform) {
      "windows-x64" -> "jolt-jni-Windows64"
      "linux-x64" -> "jolt-jni-Linux64"
      "linux-arm64" -> "jolt-jni-Linux_ARM64"
      "macos-x64" -> "jolt-jni-MacOSX64"
      "macos-arm64" -> "jolt-jni-MacOSX_ARM64"
      else -> error("No jolt-jni natives for platform $platform")
    }
}
