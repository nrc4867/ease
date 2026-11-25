package dev.chieppa.ease.gpu.ffi

/**
 * webgpu.h's usage and mode flags. They are 64-bit constants the bindings generator leaves out, so
 * their values are spelled here.
 */
internal object Flags {
  const val BUFFER_MAP_READ = 0x1L
  const val BUFFER_COPY_DST = 0x8L
  const val BUFFER_INDEX = 0x10L
  const val BUFFER_VERTEX = 0x20L
  const val BUFFER_UNIFORM = 0x40L
  const val BUFFER_STORAGE = 0x80L
  const val TEXTURE_COPY_SRC = 0x1L
  const val TEXTURE_RENDER_ATTACHMENT = 0x10L
  const val MAP_READ = 0x1L
}
