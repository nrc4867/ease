package dev.chieppa.ease.codegen.read

import com.google.devtools.ksp.symbol.KSAnnotated
import dev.chieppa.ease.codegen.model.ValueRange

/** The text of [this] declaration's `@Doc`, or null when it has none. */
internal fun KSAnnotated.doc(): String? =
  annotation(AnnotationNames.DOC)?.argument("text") as? String

/** The values of [this] declaration's `@Range`, or null when it has none. */
internal fun KSAnnotated.range(): ValueRange? {
  val annotation = annotation(AnnotationNames.RANGE) ?: return null
  val min = annotation.argument("min") as? Double ?: Double.NaN
  val max = annotation.argument("max") as? Double ?: Double.NaN
  return ValueRange(min, max)
}
