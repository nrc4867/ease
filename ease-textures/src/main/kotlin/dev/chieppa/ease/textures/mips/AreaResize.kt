package dev.chieppa.ease.textures.mips

import dev.chieppa.ease.textures.TextureUsage

/** Sums per target pixel: weighted color, plain color, alpha, plain weight and color weight. */
private const val COLOR = 0
private const val PLAIN = 3
private const val ALPHA = 6
private const val TOTAL = 7
private const val COLOR_TOTAL = 8
private const val SUMS = 9

/** Red, green and blue; alpha follows them. */
private const val COLOR_CHANNELS = 3

/**
 * Shrinks an image with a box (area) filter: each target pixel is the average of the source area it
 * covers, partial pixels weighted by how much of them it covers. Weights are whole numbers and sums
 * are doubles added in a fixed order, so the result is the same on every machine. Colors are
 * averaged weighted by alpha, so transparent pixels do not bleed their color (a fully transparent
 * area keeps its plain average); normals are averaged as vectors and renormalized.
 */
public object AreaResize {
  /** [source] shrunk to [width] by [height], no larger than it, treated as [usage] says. */
  public fun resize(
    source: LinearImage,
    width: Int,
    height: Int,
    usage: TextureUsage,
  ): LinearImage {
    require(width in 1..source.width && height in 1..source.height) {
      "Can only shrink ${source.width} x ${source.height}, not to $width x $height"
    }
    val out = FloatArray(width * height * PIXEL_FLOATS)
    val xs = Spans(source.width, width)
    val ys = Spans(source.height, height)
    val sum = DoubleArray(SUMS)
    for (y in 0 until height) {
      for (x in 0 until width) {
        sum.fill(0.0)
        for (sy in ys.first(y)..ys.last(y)) {
          val wy = ys.weight(y, sy)
          for (sx in xs.first(x)..xs.last(x)) {
            accumulate(source, sx, sy, (wy * xs.weight(x, sx)).toDouble(), usage, sum)
          }
        }
        finish(sum, usage, out, (y * width + x) * PIXEL_FLOATS)
      }
    }
    return LinearImage(width, height, out)
  }

  /** Halves [source] (rounding down, at least 1 pixel a side), as the next mip level. */
  public fun half(source: LinearImage, usage: TextureUsage): LinearImage =
    resize(source, maxOf(1, source.width / 2), maxOf(1, source.height / 2), usage)

  @Suppress("LongParameterList") // A pixel and where its weighted values go.
  private fun accumulate(
    source: LinearImage,
    x: Int,
    y: Int,
    weight: Double,
    usage: TextureUsage,
    sum: DoubleArray,
  ) {
    val at = (y * source.width + x) * PIXEL_FLOATS
    val pixels = source.rgba
    val alpha = pixels[at + COLOR_CHANNELS].toDouble()
    val colorWeight = if (usage == TextureUsage.COLOR_SRGB) weight * alpha else weight
    for (channel in 0 until COLOR_CHANNELS) {
      val stored = pixels[at + channel].toDouble()
      val value = if (usage == TextureUsage.NORMAL) stored * 2 - 1 else stored
      sum[COLOR + channel] += colorWeight * value
      sum[PLAIN + channel] += weight * value
    }
    sum[ALPHA] += weight * alpha
    sum[TOTAL] += weight
    sum[COLOR_TOTAL] += colorWeight
  }

  private fun finish(sum: DoubleArray, usage: TextureUsage, out: FloatArray, at: Int) {
    if (usage == TextureUsage.NORMAL) {
      val x = sum[PLAIN]
      val y = sum[PLAIN + 1]
      val z = sum[PLAIN + 2]
      val length = Math.sqrt(x * x + y * y + z * z)
      out[at] = if (length > 0) ((x / length + 1) / 2).toFloat() else HALF
      out[at + 1] = if (length > 0) ((y / length + 1) / 2).toFloat() else HALF
      out[at + 2] = if (length > 0) ((z / length + 1) / 2).toFloat() else 1f
    } else {
      val weighted = sum[COLOR_TOTAL] > 0
      for (channel in 0 until COLOR_CHANNELS) {
        val value =
          if (weighted) sum[COLOR + channel] / sum[COLOR_TOTAL]
          else sum[PLAIN + channel] / sum[TOTAL]
        out[at + channel] = value.toFloat()
      }
    }
    out[at + COLOR_CHANNELS] = (sum[ALPHA] / sum[TOTAL]).toFloat()
  }

  private const val HALF = 0.5f
}
