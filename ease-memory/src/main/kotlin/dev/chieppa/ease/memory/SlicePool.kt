package dev.chieppa.ease.memory

/** The slice objects of one block, handed out in order and reused once the block is reset. */
internal class SlicePool(private val arena: FrameArena) {
  private var slices = arrayOfNulls<FrameSlice>(INITIAL_SLICES)
  private var handedOut = 0

  fun reset() {
    handedOut = 0
  }

  fun next(): FrameSlice {
    if (handedOut == slices.size) slices = slices.copyOf(slices.size * 2)
    val slice = slices[handedOut] ?: FrameSlice(arena).also { slices[handedOut] = it }
    handedOut++
    return slice
  }

  private companion object {
    const val INITIAL_SLICES = 16
  }
}
