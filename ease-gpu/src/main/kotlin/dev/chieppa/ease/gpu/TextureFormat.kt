package dev.chieppa.ease.gpu

import dev.chieppa.webgpu.ffi.wgpu_h

/** The color formats Ease renders into: 8 bits a channel, unsigned, normalized, not sRGB. */
public enum class TextureFormat(internal val native: Int) {
  BGRA8Unorm(wgpu_h.WGPUTextureFormat_BGRA8Unorm()),
  RGBA8Unorm(wgpu_h.WGPUTextureFormat_RGBA8Unorm());

  internal companion object {
    /** The format whose wgpu value is [native], or null for one Ease does not render into. */
    fun of(native: Int): TextureFormat? = entries.firstOrNull { it.native == native }
  }
}
