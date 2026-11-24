package dev.chieppa.ease.annotations

/**
 * The values a number field takes, [min] to [max] inclusive. ease-codegen writes it into the JSON
 * Schemas as `minimum` and `maximum` and into `ease.d.ts` as a doc comment. A `@Quantized` field
 * spreads its bits over this range and values outside it are clamped.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public annotation class Range(val min: Double, val max: Double)
