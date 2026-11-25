package dev.chieppa.ease.memory

import java.lang.foreign.Arena
import java.lang.foreign.ValueLayout
import kotlin.test.Test
import kotlin.test.assertEquals

/** Struct fields written at their offsets from the writer's base. */
class NativeWriterTest {
  @Test
  fun `fields land at their offsets from the base, and next moves to the following struct`() {
    Arena.ofConfined().use { arena ->
      val memory = arena.allocate(64)
      val writer = NativeWriter().begin(memory)

      writer.vec3(0, 1f, 2f, 3f)
      writer.f32(12, 4f)
      writer.next(32)
      writer.u32(0, 5u)
      writer.nested(16) { writer.vec2(0, 6f, 7f) }
      writer.i32(4, -8)

      val floats = { at: Long -> memory.get(ValueLayout.JAVA_FLOAT, at) }
      assertEquals(listOf(1f, 2f, 3f, 4f), listOf(0L, 4L, 8L, 12L).map(floats))
      assertEquals(5, memory.get(ValueLayout.JAVA_INT, 32))
      assertEquals(-8, memory.get(ValueLayout.JAVA_INT, 36))
      assertEquals(listOf(6f, 7f), listOf(48L, 52L).map(floats))
    }
  }

  @Test
  fun `a matrix is copied whole, column after column`() {
    Arena.ofConfined().use { arena ->
      val memory = arena.allocate(80)
      val matrix = FloatArray(20) { it.toFloat() }

      NativeWriter().begin(memory).mat4(16, matrix, from = 4)

      assertEquals(4f, memory.get(ValueLayout.JAVA_FLOAT, 16))
      assertEquals(19f, memory.get(ValueLayout.JAVA_FLOAT, 76))
    }
  }
}
