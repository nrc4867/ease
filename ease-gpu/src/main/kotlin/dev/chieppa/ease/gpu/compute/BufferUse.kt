package dev.chieppa.ease.gpu.compute

import dev.chieppa.ease.gpu.ffi.Flags

/** What a [GpuBuffer] is for. Every buffer can be written from memory and read back. */
public enum class BufferUse(internal val flags: Long) {
  /** Read and written by compute shaders. */
  STORAGE(Flags.BUFFER_STORAGE),

  /** Read by shaders as uniforms. */
  UNIFORM(Flags.BUFFER_UNIFORM),

  /** A mesh's vertices, written by compute shaders and drawn as a vertex buffer. */
  VERTICES(Flags.BUFFER_VERTEX or Flags.BUFFER_STORAGE),

  /** A mesh's 32-bit indices, written by compute shaders and drawn as an index buffer. */
  INDICES(Flags.BUFFER_INDEX or Flags.BUFFER_STORAGE),
}
