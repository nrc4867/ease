package dev.chieppa.ease.codegen.model

/** The values a number field takes, as its `@Range` sets them: [min] to [max], both included. */
internal data class ValueRange(val min: Double, val max: Double) {
  /** Whether both ends are finite numbers with [min] below [max]. */
  val isProper: Boolean
    get() = min.isFinite() && max.isFinite() && max > min
}
