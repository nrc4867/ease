package dev.chieppa.ease.codegen.output

import dev.chieppa.ease.codegen.model.PrimitiveKind
import dev.chieppa.ease.codegen.model.TypeRef

/** How a [TypeRef] is spelled in TypeScript. */
internal object TypeScriptTypes {
  /** The interface every value of an open hierarchy is declared as. */
  const val OPEN_BASE = "ease.Typed"

  /** [type] as a TypeScript type. */
  fun of(type: TypeRef): String =
    when (type) {
      is TypeRef.Primitive -> primitive(type.kind)
      is TypeRef.Named -> type.serialName
      is TypeRef.ListOf -> "${wrapped(type.element)}[]"
      is TypeRef.MapOf -> "Record<string, ${of(type.value)}>"
      is TypeRef.Nullable -> "${of(type.inner)} | null"
      is TypeRef.Open -> OPEN_BASE
      TypeRef.Unknown -> "unknown"
    }

  private fun wrapped(type: TypeRef): String =
    if (type is TypeRef.Nullable) "(${of(type)})" else of(type)

  private fun primitive(kind: PrimitiveKind): String =
    when (kind) {
      PrimitiveKind.BOOLEAN -> "boolean"
      PrimitiveKind.INTEGER,
      PrimitiveKind.NUMBER -> "number"
      PrimitiveKind.STRING -> "string"
    }

  /** Whether [type] is, or holds, a value of an open hierarchy. */
  fun containsOpen(type: TypeRef): Boolean =
    when (type) {
      is TypeRef.Open -> true
      is TypeRef.ListOf -> containsOpen(type.element)
      is TypeRef.MapOf -> containsOpen(type.value)
      is TypeRef.Nullable -> containsOpen(type.inner)
      else -> false
    }

  /** [serialName] split into its namespace (everything before the last dot) and its name. */
  fun splitName(serialName: String): Pair<String, String> =
    serialName.substringBeforeLast('.', "") to serialName.substringAfterLast('.')

  /** [text] as a double-quoted string literal. */
  fun quoted(text: String): String = "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

  /** [value] without a trailing `.0` when it is whole. */
  fun plain(value: Double): String =
    if (value == Math.rint(value)) value.toLong().toString() else value.toString()
}
