package dev.chieppa.ease.gpu.mesh

import java.lang.foreign.MemorySegment

/** A mesh's vertex and index buffers on a device, as a pass draws them. */
internal interface MeshBuffers {
  val vertexBuffer: MemorySegment
  val indexBuffer: MemorySegment

  /** Bytes of vertices and of indices to draw from. */
  val vertexBytes: Long
  val indexBytes: Long

  /** Indices to draw, three per triangle. */
  val indexCount: Int
}
