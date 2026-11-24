package dev.chieppa.ease.codegen.output

import dev.chieppa.ease.codegen.model.DescribedType
import dev.chieppa.ease.codegen.model.SerialField
import dev.chieppa.ease.codegen.model.SerialType
import dev.chieppa.ease.codegen.model.TYPE_KEY
import dev.chieppa.ease.codegen.model.TypeKind
import dev.chieppa.ease.codegen.model.ValueRange

/**
 * A module's part of `ease.d.ts`: one block per type, so the game's build can join the parts of
 * every module and keep each type once. A type is declared in a namespace named by its serial
 * name's dotted prefix, `ease.Destroy` as `Destroy` in `namespace ease`; `@Doc` becomes its doc
 * comment, and `@Range` the `@minimum` and `@maximum` tags of its field's.
 */
internal object TypeScriptSource {
  /** The line that starts each block, followed by the type's serial name. */
  const val BLOCK_MARKER = "// ease-type: "

  private const val INDENT = "  "

  /** The part for [types]: every type they reach, in serial name order. */
  fun of(types: List<DescribedType>): String {
    val reachable =
      types.flatMap { it.reachable.values }.associateBy { it.serialName }.toSortedMap()
    val blocks = reachable.values.map { block(it) }.toMutableList()
    if (reachable.values.any { usesOpen(it) }) blocks.add(0, openBlock())
    return blocks.joinToString("\n")
  }

  private fun block(type: SerialType): String = buildString {
    appendLine(BLOCK_MARKER + type.serialName)
    val (namespace, name) = TypeScriptTypes.splitName(type.serialName)
    val indent = if (namespace.isEmpty()) "" else INDENT
    if (namespace.isNotEmpty()) appendLine("declare namespace $namespace {")
    appendDoc(indent, listOfNotNull(type.doc))
    declaration(type, name).lines().forEach { appendLine(if (it.isEmpty()) it else indent + it) }
    if (namespace.isNotEmpty()) appendLine("}")
  }

  private fun declaration(type: SerialType, name: String): String =
    when (type.kind) {
      TypeKind.CLASS -> classDeclaration(type, name)
      TypeKind.ENUM ->
        "type $name = ${type.values.joinToString(" | ") { TypeScriptTypes.quoted(it) }};"
      TypeKind.VALUE -> "type $name = ${TypeScriptTypes.of(type.fields.single().type)};"
      TypeKind.SEALED -> "type $name = ${type.subtypes.joinToString(" | ")};"
    }

  private fun classDeclaration(type: SerialType, name: String): String = buildString {
    appendLine("interface $name {")
    if (type.discriminated)
      appendLine("$INDENT$TYPE_KEY: ${TypeScriptTypes.quoted(type.serialName)};")
    type.fields.forEach { appendField(it) }
    append("}")
  }

  private fun StringBuilder.appendField(field: SerialField) {
    val tags = field.range?.let { rangeTags(it) }.orEmpty()
    appendDoc(INDENT, listOfNotNull(field.doc) + tags)
    val optional = if (field.optional) "?" else ""
    appendLine("$INDENT${field.name}$optional: ${TypeScriptTypes.of(field.type)};")
  }

  private fun rangeTags(range: ValueRange): List<String> =
    listOf(
      "@minimum ${TypeScriptTypes.plain(range.min)}",
      "@maximum ${TypeScriptTypes.plain(range.max)}",
    )

  private fun StringBuilder.appendDoc(indent: String, lines: List<String>) {
    if (lines.isEmpty()) return
    val safe = lines.map { it.replace("*/", "* /") }
    if (safe.size == 1) {
      appendLine("$indent/** ${safe.single()} */")
      return
    }
    appendLine("$indent/**")
    safe.forEach { appendLine("$indent * $it") }
    appendLine("$indent */")
  }

  private fun usesOpen(type: SerialType): Boolean =
    type.fields.any { TypeScriptTypes.containsOpen(it.type) }

  private fun openBlock(): String =
    """
    |${BLOCK_MARKER}${TypeScriptTypes.OPEN_BASE}
    |declare namespace ease {
    |  /** A value of an open hierarchy, as a command or a component: its serial name under type. */
    |  interface Typed {
    |    $TYPE_KEY: string;
    |    [field: string]: unknown;
    |  }
    |}
    """
      .trimMargin() + "\n"
}
