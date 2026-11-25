package dev.chieppa.ease.net.prediction

import kotlin.math.floor

/**
 * Keeps a client's tick ahead of the host's by half the round trip plus a buffer, so its input
 * arrives just before the host needs it. The host reports, in each snapshot, how many ticks early
 * the client's input reached it (its slack, negative when late); the client [observe]s each report
 * and runs [ticksThisStep] ticks for each host tick, slightly more or fewer than one while the
 * slack is off the buffer. The first report, and any showing the client far behind, make it
 * [observe] a jump instead: ticks to skip ahead at once. Reports about input sent before a jump are
 * ignored. Nothing here allocates.
 */
public class ClockSync(private val settings: ClockSettings = ClockSettings()) {
  private val readings = LongArray(settings.window)
  private var readingCount = 0
  private var nextReading = 0
  private var aligned = false
  private var ignoreBefore = Long.MIN_VALUE
  private var carry = 0.0

  /** The client's step rate relative to the host's. */
  public var rate: Double = 1.0
    private set

  /** The earliest slack among the last reports, or 0 before any. */
  public val slack: Long
    get() = if (readingCount == 0) 0L else earliest()

  /**
   * Takes the host's report that the client's input of [inputTick] arrived [slack] ticks early, and
   * returns how many ticks the client should skip ahead before its tick [clientTick], usually 0.
   */
  public fun observe(slack: Long, inputTick: Long, clientTick: Long): Long {
    if (inputTick < ignoreBefore) return 0L
    val behind = settings.bufferTicks - slack
    val jumps = !aligned || behind > settings.resyncTicks
    if (jumps) jump(maxOf(0L, behind), clientTick) else read(slack)
    return if (jumps) maxOf(0L, behind) else 0L
  }

  private fun jump(ticks: Long, clientTick: Long) {
    aligned = true
    ignoreBefore = clientTick + ticks
    readingCount = 0
    rate = 1.0
  }

  private fun read(slack: Long) {
    readings[nextReading] = slack
    nextReading = (nextReading + 1) % readings.size
    readingCount = minOf(readingCount + 1, readings.size)
    adjust()
  }

  /** How many ticks the client runs for this host tick: 1 on time, sometimes 0 or 2 off it. */
  public fun ticksThisStep(): Int {
    carry += rate
    val ticks = floor(carry)
    carry -= ticks
    return ticks.toInt()
  }

  private fun adjust() {
    val error = settings.bufferTicks - earliest()
    val change = (error * settings.gain).coerceIn(-settings.maxAdjust, settings.maxAdjust)
    rate = 1.0 + change
  }

  private fun earliest(): Long {
    var least = Long.MAX_VALUE
    for (index in 0 until readingCount) least = minOf(least, readings[index])
    return least
  }
}
