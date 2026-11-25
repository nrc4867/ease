package dev.chieppa.ease.build

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** jolt-jni's per-platform artifacts are named by platform, and listed as one when shipped. */
class JoltNativesTest {
  @Test
  fun `each platform has its jolt-jni artifact`() {
    assertEquals("jolt-jni-Windows64", JoltNatives.artifact("windows-x64"))
    assertEquals("jolt-jni-Linux64", JoltNatives.artifact("linux-x64"))
    assertEquals("jolt-jni-Linux_ARM64", JoltNatives.artifact("linux-arm64"))
    assertEquals("jolt-jni-MacOSX64", JoltNatives.artifact("macos-x64"))
    assertEquals("jolt-jni-MacOSX_ARM64", JoltNatives.artifact("macos-arm64"))
    assertFailsWith<IllegalStateException> { JoltNatives.artifact("plan9-mips") }
  }

  @Test
  fun `any platform's artifact is listed as the classes artifact, and nothing else changes`() {
    val classes = JoltNatives.classes("7.0.0")

    assertEquals(classes, JoltNatives.portable("com.github.stephengold:jolt-jni-Windows64:7.0.0"))
    assertEquals(
      classes,
      JoltNatives.portable("com.github.stephengold:jolt-jni-MacOSX_ARM64:7.0.0"),
    )
    assertEquals(classes, JoltNatives.portable(classes))
    assertEquals("org.lwjgl:lwjgl:3.4.3", JoltNatives.portable("org.lwjgl:lwjgl:3.4.3"))
  }
}
