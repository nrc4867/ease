package dev.chieppa.ease.memory

import dev.chieppa.ease.testing.AllocationBudget
import java.lang.foreign.ValueLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Slices handed out per frame, checked in a debug arena and reused in a release one. */
class FrameArenaTest {
  @Test
  fun `a checked slice used after its frame throws naming where it was taken`() {
    FrameArena(bytesPerFrame = 1024, checked = true).use { arena ->
      val slice = arena.take(16, "test.uniforms")
      slice.segment.set(ValueLayout.JAVA_INT, 0, 7)

      arena.beginFrame()

      val stale = assertFailsWith<StaleSliceException> { slice.segment }
      assertTrue("test.uniforms" in stale.message.orEmpty(), stale.message)
      assertTrue("frame 0" in stale.message.orEmpty(), stale.message)
    }
  }

  @Test
  fun `a writer on a checked slice refuses to write after the frame`() {
    FrameArena(bytesPerFrame = 1024, checked = true).use { arena ->
      val writer = NativeWriter().begin(arena.take(16, "test.writer"))
      arena.beginFrame()

      assertFailsWith<StaleSliceException> { writer.f32(0, 1f) }
    }
  }

  @Test
  fun `slices are aligned, zeroed and laid one after another`() {
    FrameArena(bytesPerFrame = 1024, checked = true).use { arena ->
      val first = arena.take(4, "a")
      first.segment.set(ValueLayout.JAVA_INT, 0, -1)
      val second = arena.take(8, "b", alignment = 64)

      assertEquals(0L, second.segment.address() % 64)
      assertEquals(72L, arena.bytesUsed)
      arena.beginFrame()
      arena.beginFrame()
      assertEquals(0, arena.take(4, "c").segment.get(ValueLayout.JAVA_INT, 0))
    }
  }

  @Test
  fun `a frame that outgrows its block fails naming the taker`() {
    FrameArena(bytesPerFrame = 64, checked = false).use { arena ->
      arena.take(48, "first")

      val full = assertFailsWith<IllegalStateException> { arena.take(32, "test.instances") }
      assertTrue("test.instances" in full.message.orEmpty(), full.message)
    }
  }

  @Test
  fun `a release arena hands the same slice objects back each time a block comes round`() {
    FrameArena(framesInFlight = 2, bytesPerFrame = 256, checked = false).use { arena ->
      val first = arena.take(32, "a")
      arena.beginFrame()
      arena.beginFrame()

      assertSame(first, arena.take(32, "a"))
    }
  }

  @Test
  fun `a release frame taking and writing the same slices as the last allocates nothing`() {
    FrameArena(bytesPerFrame = 4096, checked = false).use { arena ->
      val writer = NativeWriter()
      val matrix = FloatArray(16) { it.toFloat() }

      AllocationBudget.assertNoAllocation {
        arena.beginFrame()
        writer.begin(arena.take(128, "frame"))
        writer.mat4(0, matrix)
        writer.vec3(64, 1f, 2f, 3f)
        writer.begin(arena.take(8, "submit"))
        writer.i32(0, 1)
      }
    }
  }
}
