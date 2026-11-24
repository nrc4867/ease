package dev.chieppa.ease.codegen.rules

import dev.chieppa.ease.codegen.model.IDENTIFIER
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
    listOfNotNull(nameProblem(type)) + type.fields.mapNotNull { field -> rangeProblem(field) }

  /** TypeScript declares a type in the namespaces its serial name's dotted parts name. */
  private fun nameProblem(type: SerialType): Problem? =
    if (type.serialName.split('.').all { IDENTIFIER.matches(it) }) {
      null
    } else {
      Problem(null, "serial name ${type.serialName} needs each dotted part to be an identifier")
    }

  private fun rangeProblem(field: SerialField): Problem? {
    val range = field.range ?: return null
    return when {
      !isNumber(field.type) ->
        Problem(field.propertyName, "@Range goes on an integer or a number field")
      !range.isProper -> Problem(field.propertyName, "@Range needs finite min < max")
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
