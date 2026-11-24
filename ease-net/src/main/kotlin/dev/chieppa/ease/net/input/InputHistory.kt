package dev.chieppa.ease.net.input

/**
 * A client's last few ticks of input, newest first, which every input packet repeats. Ticks must
 * follow one another; a gap starts the history again from the new tick.
 */
internal class InputHistory(layout: InputLayout) {
  private val frames = Array(InputPacket.MAX_FRAMES) { InputFrame(layout) }
  private var newest = 0
  private var count = 0

  /** The tick of the newest frame, or -1 before the first. */
  var newestTick: Long = -1L
    private set

  /** Records [frame] as the input of [tick]. */
  fun record(tick: Long, frame: InputFrame) {
    require(tick > newestTick) { "Input ticks rise: $tick follows $newestTick" }
    if (tick != newestTick + 1) count = 0
    newest = (newest + 1) % frames.size
    frames[newest].copyFrom(frame)
    count = minOf(count + 1, frames.size)
    newestTick = tick
  }

  /** Copies up to [most] of the newest frames into [packet], newest first. */
  fun fill(packet: InputPacket, most: Int) {
    packet.newestTick = newestTick
    packet.count = minOf(most, count)
    for (index in 0 until packet.count) {
      packet.frames[index].copyFrom(frames[(newest - index + frames.size) % frames.size])
    }
  }
}
