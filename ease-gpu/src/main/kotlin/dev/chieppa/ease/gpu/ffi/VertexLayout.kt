package dev.chieppa.ease.gpu.ffi

/**
 * A vertex buffer's layout: [stride] bytes a vertex, each of [attributes] a float vector of its
 * component count at its byte offset, bound at the shader location numbered by its position.
 */
internal class VertexLayout(val stride: Long, val attributes: List<VertexAttribute>)
