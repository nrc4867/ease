package dev.chieppa.ease.physics.jolt

import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/**
 * Reads and writes fields of Jolt objects in place, at their native addresses, where jolt-jni has
 * no accessor that avoids making an object. Every address comes from jolt-jni (`va()` or a
 * collector's hit), and every offset is that of a field in Jolt's single-precision build.
 */
internal object JoltMemory {
  private val everything: MemorySegment = MemorySegment.NULL.reinterpret(Long.MAX_VALUE)
  private val float = ValueLayout.JAVA_FLOAT_UNALIGNED
  private val int = ValueLayout.JAVA_INT_UNALIGNED

  fun putFloat(address: Long, offset: Long, value: Float) {
    everything.set(float, address + offset, value)
  }

  fun getInt(address: Long, offset: Long): Int = everything.get(int, address + offset)
}
