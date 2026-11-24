package dev.chieppa.ease.build

import org.gradle.api.provider.ProviderFactory

/** The LWJGL natives jars a build takes, for the platform [NativePlatform] picks. */
object LwjglNatives {
  /** The classifier of LWJGL's natives jars, such as `natives-windows` or `natives-macos-arm64`. */
  fun classifier(providers: ProviderFactory): String =
    when (val platform = NativePlatform.of(providers)) {
      "windows-x64" -> "natives-windows"
      "linux-x64" -> "natives-linux"
      "linux-arm64" -> "natives-linux-arm64"
      "macos-x64" -> "natives-macos"
      "macos-arm64" -> "natives-macos-arm64"
      else -> error("No LWJGL natives for platform $platform")
    }
}
