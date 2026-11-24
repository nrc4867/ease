package dev.chieppa.ease.codegen.output

import dev.chieppa.ease.codegen.model.DescribedType
import dev.chieppa.ease.codegen.model.SerialField
import dev.chieppa.ease.codegen.model.SerialType
import dev.chieppa.ease.codegen.model.TYPE_KEY
import dev.chieppa.ease.codegen.model.TypeKind
import dev.chieppa.ease.codegen.model.ValueRange

/**
 * A module's part of `ease.d.ts`: one block per type, so the game's build can join the parts of
 * every module and keep each type once. A block starts with a marker line naming its type and
 * whether the module declares the type or only reaches it, and the build prefers a declared block.
 *
 * A type is declared in a namespace named by its serial name's dotted prefix, `ease.Destroy` as
 * `Destroy` in `namespace ease`; `@Doc` becomes its doc comment, and `@Range` the `@minimum` and
 * `@maximum` tags of its field's. A class that belongs to a polymorphic base also gets
 * `DestroyTyped`, which adds the `type` property; a sealed hierarchy is a union of those.
 */
internal object TypeScriptSource {
  private const val INDENT = "  "

  /** The part for [types]: every type they reach, in serial name order. */
  fun of(types: List<DescribedType>): String {
    val declared = types.map { it.type.serialName }.toSet()
    val reachable =
      types.flatMap { it.reachable.values }.associateBy { it.serialName }.toSortedMap()
    val blocks = reachable.values.map { block(it, it.serialName in declared, reachable) }
    val open = if (reachable.values.any { usesOpen(it) }) listOf(openBlock()) else emptyList()
    return (open + blocks).joinToString("\n")
  }

  private fun block(type: SerialType, declared: Boolean, all: Map<String, SerialType>): String =
    buildString {
      appendLine(
        (if (declared) ResourceFiles.DECLARED_MARKER else ResourceFiles.REACHED_MARKER) +
          type.serialName
      )
      val (namespace, name) = TypeScriptTypes.splitName(type.serialName)
      val indent = if (namespace.isEmpty()) "" else INDENT
      if (namespace.isNotEmpty()) appendLine("declare namespace $namespace {")
      val lines = StringBuilder()
      lines.appendDoc("", listOfNotNull(type.doc))
      lines.appendLine(declaration(type, name, all))
      val text = lines.toString().trimEnd()
      text.lines().forEach { appendLine(if (it.isEmpty()) it else indent + it) }
      if (namespace.isNotEmpty()) appendLine("}")
    }

  private fun declaration(type: SerialType, name: String, all: Map<String, SerialType>): String =
    when (type.kind) {
      TypeKind.CLASS -> classDeclaration(type, name)
      TypeKind.ENUM ->
        "type $name = ${type.values.joinToString(" | ") { TypeScriptTypes.quoted(it) }};"
      TypeKind.VALUE -> "type $name = ${TypeScriptTypes.of(type.fields.single().type)};"
      TypeKind.SEALED -> "type $name = ${union(type, all)};"
    }

  /** The subtypes of a sealed hierarchy, each as its `type`-carrying form; `never` for none. */
  private fun union(type: SerialType, all: Map<String, SerialType>): String =
    type.subtypes
      .map { if (all[it]?.kind == TypeKind.CLASS) TypeScriptTypes.typedName(it) else it }
      .ifEmpty { listOf("never") }
      .joinToString(" | ")

  private fun classDeclaration(type: SerialType, name: String): String = buildString {
    appendLine("interface $name {")
    type.fields.forEach { appendField(it) }
    append("}")
    if (type.discriminated) {
      appendLine()
      appendLine("interface ${name}Typed extends $name {")
      appendLine("$INDENT$TYPE_KEY: ${TypeScriptTypes.quoted(type.serialName)};")
      append("}")
    }
  }

  private fun StringBuilder.appendField(field: SerialField) {
    val tags = field.range?.let { rangeTags(it) }.orEmpty()
    appendDoc(INDENT, listOfNotNull(field.doc) + tags)
    val optional = if (field.optional) "?" else ""
    val property = TypeScriptTypes.propertyName(field.name)
    appendLine("$INDENT$property$optional: ${TypeScriptTypes.of(field.type)};")
  }

  private fun rangeTags(range: ValueRange): List<String> =
    listOf("@minimum ${NumberText.plain(range.min)}", "@maximum ${NumberText.plain(range.max)}")

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
    |${ResourceFiles.DECLARED_MARKER}${TypeScriptTypes.OPEN_BASE}
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
