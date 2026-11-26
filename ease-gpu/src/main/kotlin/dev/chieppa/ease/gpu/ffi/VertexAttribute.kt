package dev.chieppa.ease.gpu.ffi

/**
 * One vector at [offset] bytes into a vertex: [components] (2 to 4) floats, or with [unsigned16]
 * four 16-bit unsigned integers the shader reads as `vec4<u32>`.
 */
internal class VertexAttribute(
  val components: Int,
  val offset: Long,
  val unsigned16: Boolean = false,
)
