package dev.chieppa.ease.codegen.output

import kotlin.math.abs

/** How the outputs spell a range end, so JSON and TypeScript agree on every number. */
internal object NumberText {
  /** The largest magnitude below which every whole double is an exact `Long`. */
  private const val MAX_EXACT = 9.0e15

  /** [value] as a `Long` when it is whole and exact as one, so `0.0` reads as `0`; else null. */
  fun whole(value: Double): Long? =
    if (value == Math.rint(value) && abs(value) < MAX_EXACT) value.toLong() else null

  /** [value] as text: without a trailing `.0` when whole, and never saturated. */
  fun plain(value: Double): String = whole(value)?.toString() ?: value.toString()
}
