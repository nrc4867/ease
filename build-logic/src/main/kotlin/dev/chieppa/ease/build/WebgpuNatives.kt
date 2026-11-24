package dev.chieppa.ease.build

import org.gradle.api.provider.ProviderFactory

/** The webgpu-native-ffm natives jar a build takes, for the platform [NativePlatform] picks. */
object WebgpuNatives {
  /** The classifier of the natives jar to take, such as `natives-windows-x64`. */
  fun classifier(providers: ProviderFactory): String = "natives-${NativePlatform.of(providers)}"
}
