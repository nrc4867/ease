package dev.chieppa.ease.codegen.rules

import dev.chieppa.ease.codegen.model.PrimitiveKind
import dev.chieppa.ease.codegen.model.SerialField
import dev.chieppa.ease.codegen.model.SerialType
import dev.chieppa.ease.codegen.model.TypeRef

/**
 * What makes a [SerialType]'s annotations meaningless, each problem naming its field. The processor
 * fails the build with them and writes nothing for the type.
 */
internal object SchemaRules {
  /** Every problem with [type], empty when its outputs can be written. */
  fun problems(type: SerialType): List<Problem> =
    type.fields.mapNotNull { field -> rangeProblem(field) }

  private fun rangeProblem(field: SerialField): Problem? {
    val range = field.range ?: return null
    return when {
      !isNumber(field.type) -> Problem(field.name, "@Range goes on an integer or a number field")
      !range.isProper -> Problem(field.name, "@Range needs finite min < max")
      else -> null
    }
  }

  private fun isNumber(type: TypeRef): Boolean =
    when (type) {
      is TypeRef.Nullable -> isNumber(type.inner)
      is TypeRef.Primitive ->
        type.kind == PrimitiveKind.INTEGER || type.kind == PrimitiveKind.NUMBER
      else -> false
    }
}
