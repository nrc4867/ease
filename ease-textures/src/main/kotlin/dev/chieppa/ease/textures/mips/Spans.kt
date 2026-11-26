package dev.chieppa.ease.textures.mips

/**
 * How [target] pixels along one axis cover [source] pixels when [source] is shrunk to [target]: in
 * units of 1 / [target] of a source pixel, so every overlap is a whole number. The products are
 * taken as longs, so no image wide enough to hold in memory overflows them.
 */
internal class Spans(source: Int, target: Int) {
  private val source = source.toLong()
  private val target = target.toLong()

  /** The first source pixel target pixel [index] covers. */
  fun first(index: Int): Int = (index * source / target).toInt()

  /** The last source pixel target pixel [index] covers. */
  fun last(index: Int): Int = (((index + 1) * source - 1) / target).toInt()

  /** How much of source pixel [pixel] target pixel [index] covers, in 1 / [target] units. */
  fun weight(index: Int, pixel: Int): Long {
    val start = maxOf(index * source, pixel * target)
    val end = minOf((index + 1) * source, (pixel + 1) * target)
    return end - start
  }
}
