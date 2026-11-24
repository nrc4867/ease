package dev.chieppa.ease.build

import org.gradle.api.provider.ProviderFactory

/**
 * The platform whose native libraries a build takes: this machine's, or the one named by
 * `-Pnatives` (`windows-x64`, `linux-x64`, `linux-arm64`, `macos-x64` or `macos-arm64`) when
 * packaging for another. Each native library resolves only that platform's jar, never all of them.
 */
object NativePlatform {
  const val PROPERTY = "natives"

  /** The platform to take natives for, such as `windows-x64`. */
  fun of(providers: ProviderFactory): String = providers.gradleProperty(PROPERTY).orNull ?: host()

  /** The platform of the machine running the build. */
  fun host(): String {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    val arm = arch == "aarch64" || arch == "arm64"
    return when {
      os.startsWith("windows") -> "windows-x64"
      os.startsWith("mac") -> if (arm) "macos-arm64" else "macos-x64"
      else -> if (arm) "linux-arm64" else "linux-x64"
    }
  }

  /** Whether the machine running the build is a Mac. */
  fun hostIsMac(): Boolean = host().startsWith("macos")
}
