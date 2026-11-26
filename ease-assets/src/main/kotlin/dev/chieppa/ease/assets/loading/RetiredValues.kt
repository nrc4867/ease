package dev.chieppa.ease.assets.loading

/**
 * Values a reload replaced that hold resources, waiting to be closed. A value retired before one
 * [boundary] is closed at the next one, so a frame that drew it from a snapshot taken before the
 * reload has finished with it. Not shared between threads.
 */
internal class RetiredValues {
  private val waiting = ArrayList<AutoCloseable>()
  private val closing = ArrayList<AutoCloseable>()

  /** Keeps [value] for closing when it is [AutoCloseable]; other values need nothing. */
  fun add(value: Any) {
    if (value is AutoCloseable) waiting += value
  }

  /**
   * Closes what was retired before the last boundary; what was retired since waits for the next.
   */
  fun boundary() {
    for (index in closing.indices) closing[index].close()
    closing.clear()
    for (index in waiting.indices) closing += waiting[index]
    waiting.clear()
  }

  /** How many values wait to be closed. */
  val size: Int
    get() = waiting.size + closing.size
}
