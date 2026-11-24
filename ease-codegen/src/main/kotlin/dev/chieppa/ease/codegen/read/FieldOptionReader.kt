package dev.chieppa.ease.codegen.read

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import dev.chieppa.ease.codegen.model.FieldAudience
import dev.chieppa.ease.codegen.model.FieldModel
import dev.chieppa.ease.codegen.model.Quantization

/**
 * Reads one kind of field annotation into a [FieldModel]. The [ComponentReader] runs every reader
 * it is given over each property, so a new annotation is a new reader, not a change to the others.
 */
internal fun interface FieldOptionReader {
  /** [field] with whatever [property]'s annotation of this reader's kind sets. */
  fun read(property: KSPropertyDeclaration, field: FieldModel): FieldModel
}

/** `@Quantized(bits, min, max)`. */
internal val quantizedReader = FieldOptionReader { property, field ->
  val annotation = property.annotation(AnnotationNames.QUANTIZED) ?: return@FieldOptionReader field
  val bits = annotation.argument("bits") as? Int ?: 0
  val min = annotation.argument("min") as? Float ?: 0f
  val max = annotation.argument("max") as? Float ?: 0f
  field.copy(quantization = Quantization(bits, min, max))
}

/** `@Interpolated`. */
internal val interpolatedReader = FieldOptionReader { property, field ->
  val interpolated = property.annotation(AnnotationNames.INTERPOLATED) != null
  if (interpolated) field.copy(interpolated = true) else field
}

/** `@ReplicatedTo(audience)`. */
internal val audienceReader = FieldOptionReader { property, field ->
  val annotation =
    property.annotation(AnnotationNames.REPLICATED_TO) ?: return@FieldOptionReader field
  val name = annotation.enumArgument("audience")
  val audience = FieldAudience.entries.firstOrNull { it.name == name } ?: FieldAudience.EVERYONE
  field.copy(audience = audience)
}

/** The readers of the replication field options. */
internal val replicationOptionReaders: List<FieldOptionReader> =
  listOf(quantizedReader, interpolatedReader, audienceReader)
