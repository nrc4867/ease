package dev.chieppa.ease.gpu.ffi

/**
 * A vertex buffer's layout: [stride] bytes a vertex, each of [attributes] at its byte offset, bound
 * at the shader location [firstLocation] plus its position in the list.
 */
internal class VertexLayout(
  val stride: Long,
  val attributes: List<VertexAttribute>,
  val firstLocation: Int = 0,
)
