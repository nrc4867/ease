package dev.chieppa.ease.codegen.model

/** A float's quantization: [bits] bits spread over [min] to [max]. */
internal data class Quantization(val bits: Int, val min: Float, val max: Float)
