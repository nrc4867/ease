package dev.chieppa.ease.codegen.rules

import dev.chieppa.ease.codegen.model.ClassVisibility
import dev.chieppa.ease.codegen.model.ComponentModel
import dev.chieppa.ease.codegen.model.FieldModel

/**
 * What makes a [ComponentModel] impossible to replicate, each problem naming its field when it has
 * one. The processor writes nothing for a model with problems and fails the build with them.
 */
internal object ModelRules {
  /** The most fields a component may replicate: its change mask is one `Int`. */
  const val MAX_FIELDS = 32

  /** The bits a quantized float may take. */
  val QUANTIZED_BITS = 1..30

  /** Every problem with [model], empty when it can replicate. */
  fun problems(model: ComponentModel): List<Problem> = classProblems(model) + fieldProblems(model)

  private fun classProblems(model: ComponentModel): List<Problem> = buildList {
    if (!model.isComponent) add(Problem(null, "a replicated class must implement Component"))
    if (!model.hasTypeCompanion) {
      add(Problem(null, "a replicated class needs a companion object that is its ComponentType"))
    }
    if (!model.hasNoArgumentConstructor) {
      add(
        Problem(null, "a replicated class needs a public or internal constructor without arguments")
      )
    }
    if (model.visibility == ClassVisibility.HIDDEN) {
      add(Problem(null, "a replicated class and every class around it must be public or internal"))
    }
    if (model.fields.size > MAX_FIELDS) {
      add(Problem(null, "a replicated class has at most $MAX_FIELDS fields"))
    }
  }

  private fun fieldProblems(model: ComponentModel): List<Problem> =
    model.fields.flatMap { field -> problems(field) }

  private fun problems(field: FieldModel): List<Problem> = buildList {
    if (field.kind == null) add(Problem(field.name, unsupported(field)))
    if (!field.settable) {
      add(Problem(field.name, "a replicated field needs a public or internal setter"))
    }
    val quantization = field.quantization ?: return@buildList
    if (quantization.bits !in QUANTIZED_BITS) {
      add(Problem(field.name, "@Quantized bits must be 1 to 30, not ${quantization.bits}"))
    }
    val finite = quantization.min.isFinite() && quantization.max.isFinite()
    if (!finite || quantization.max <= quantization.min) {
      add(Problem(field.name, "@Quantized needs finite min < max"))
    }
  }

  private fun unsupported(field: FieldModel): String =
    when {
      field.nullable -> "a replicated field may not be nullable"
      field.quantization != null -> "@Quantized goes on a Float, not ${field.typeName}"
      else -> "a replicated field is a Float, Int or Boolean, not ${field.typeName}"
    }
}
