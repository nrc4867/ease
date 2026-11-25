package dev.chieppa.ease.gpu.ffi

/** One float vector of [components] (2 to 4) at [offset] bytes into a vertex. */
internal class VertexAttribute(val components: Int, val offset: Long)
