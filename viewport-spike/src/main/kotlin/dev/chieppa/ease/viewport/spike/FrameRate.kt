package dev.chieppa.ease.viewport.spike

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue

private const val NANOS_PER_SECOND = 1_000_000_000L

/**
 * Frames counted each second, from whichever thread calls [tick]: [perSecond] is the count over the
 * last whole second, as Compose state the UI shows.
 */
internal class FrameRate(private val now: () -> Long = System::nanoTime) {
  var perSecond by mutableFloatStateOf(0f)
    private set

  private var windowStart = now()
  private var frames = 0

  @Synchronized
  fun tick() {
    frames++
    val elapsed = now() - windowStart
    if (elapsed >= NANOS_PER_SECOND) {
      perSecond = frames * NANOS_PER_SECOND.toFloat() / elapsed
      frames = 0
      windowStart += elapsed
    }
  }
}
