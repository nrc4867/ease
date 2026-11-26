package dev.chieppa.ease.textures.bc

import java.util.concurrent.ForkJoinPool
import kotlin.test.Test
import kotlin.test.assertContentEquals

/** The encoders' bytes depend only on the pixels, never on runs or thread counts. */
class BlockDeterminismTest {
  private val image =
    TestImages.of(48, 40) { x, y, c ->
      if (c == 3) (x * 5 + y) % 256 else (x * 37 + y * 11 * (c + 1)) % 256
    }

  private fun <T> onOneThread(work: () -> T): T {
    val pool = ForkJoinPool(1)
    try {
      return pool.submit(work).get()
    } finally {
      pool.shutdown()
    }
  }

  @Test
  fun `BC7 gives the same bytes every run and on one thread`() {
    val first = Bc7.encode(image.pixels, image.width, image.height)
    assertContentEquals(first, Bc7.encode(image.pixels, image.width, image.height))
    assertContentEquals(first, onOneThread { Bc7.encode(image.pixels, image.width, image.height) })
  }

  @Test
  fun `BC5 gives the same bytes every run and on one thread`() {
    val first = Bc5.encode(image.pixels, image.width, image.height)
    assertContentEquals(first, Bc5.encode(image.pixels, image.width, image.height))
    assertContentEquals(first, onOneThread { Bc5.encode(image.pixels, image.width, image.height) })
  }
}
