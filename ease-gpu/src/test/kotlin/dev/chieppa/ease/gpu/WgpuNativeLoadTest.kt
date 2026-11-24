package dev.chieppa.ease.gpu

import dev.chieppa.webgpu.WgpuNative
import dev.chieppa.webgpu.ffi.wgpu_h
import kotlin.test.Test
import kotlin.test.assertTrue

class WgpuNativeLoadTest {
  @Test
  fun `wgpu-native loads from this platform's natives jar`() {
    WgpuNative.load()

    val version = wgpu_h.wgpuGetVersion()

    assertTrue(version > 0, "wgpuGetVersion returned $version")
  }
}
