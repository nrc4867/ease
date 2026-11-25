package dev.chieppa.ease.memory

import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/** Byte offsets of a vector's y, z and w from its x. */
private const val Y = 4L
private const val Z = 8L
private const val W = 12L

/**
 * A reusable cursor that writes structs into native memory in the platform's byte order, which is
 * little-endian on every platform Ease runs on, as WGSL expects. Point it at memory with [begin];
 * each write names its byte offset from [base], the start of the struct being written, and [next]
 * moves [base] on to the following struct of an array. Writing allocates nothing, so one writer
 * serves a whole frame. A writer begun on a [FrameSlice] of a checked arena throws once the slice's
 * frame is over.
 */
@Suppress("TooManyFunctions") // One write per WGSL type, each a line.
public class NativeWriter {
  private var target: MemorySegment = MemorySegment.NULL
  private var slice: FrameSlice? = null

  /** Byte offset of the struct being written, from the start of the memory begun on. */
  public var base: Long = 0L

  /** Writes into [slice] from its start. */
  public fun begin(slice: FrameSlice): NativeWriter {
    target = slice.segment
    this.slice = slice
    base = 0L
    return this
  }

  /** Writes into [segment] from its start. */
  public fun begin(segment: MemorySegment): NativeWriter {
    target = segment
    slice = null
    base = 0L
    return this
  }

  /** Moves [base] on by [stride] bytes, to the next struct of an array. */
  public fun next(stride: Long) {
    base += stride
  }

  /** Writes a `f32` at [offset]. */
  public fun f32(offset: Long, value: Float) {
    check()
    target.set(ValueLayout.JAVA_FLOAT, base + offset, value)
  }

  /** Writes an `i32` at [offset]. */
  public fun i32(offset: Long, value: Int) {
    check()
    target.set(ValueLayout.JAVA_INT, base + offset, value)
  }

  /** Writes a `u32` at [offset]. */
  public fun u32(offset: Long, value: UInt) {
    i32(offset, value.toInt())
  }

  /** Writes a `vec2f` at [offset]. */
  public fun vec2(offset: Long, x: Float, y: Float) {
    f32(offset, x)
    f32(offset + Y, y)
  }

  /** Writes a `vec3f` at [offset]: three floats, leaving the fourth slot of its 16 bytes alone. */
  public fun vec3(offset: Long, x: Float, y: Float, z: Float) {
    vec2(offset, x, y)
    f32(offset + Z, z)
  }

  /** Writes a `vec4f` at [offset]. */
  public fun vec4(offset: Long, x: Float, y: Float, z: Float, w: Float) {
    vec3(offset, x, y, z)
    f32(offset + W, w)
  }

  /** Writes a column-major `mat4x4f` at [offset] from the 16 floats of [matrix] at [from]. */
  public fun mat4(offset: Long, matrix: FloatArray, from: Int = 0) {
    check()
    MemorySegment.copy(matrix, from, target, ValueLayout.JAVA_FLOAT, base + offset, MAT4_FLOATS)
  }

  /** Runs [write] with [base] moved [offset] bytes on, for a struct nested inside this one. */
  public inline fun nested(offset: Long, write: () -> Unit) {
    base += offset
    try {
      write()
    } finally {
      base -= offset
    }
  }

  private fun check() {
    slice?.checkCurrent()
  }
}
