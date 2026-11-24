package dev.chieppa.ease.build.types

/**
 * Joins the TypeScript parts every module's `ease-codegen` run writes into one `ease.d.ts`. A part
 * is a list of blocks, each starting with a marker line that names its type and says whether the
 * module declares the type or only reaches it. A type several parts carry is kept once, from the
 * block of the module that declares it, else from the first part that reaches it; the blocks come
 * out in type-name order. The constants are copies of `GeneratedFiles` in `ease-annotations`, which
 * this build cannot depend on; a test holds them equal.
 */
object TypeFragments {
  /** The resource directory a module's part is in. */
  const val DIRECTORY = "META-INF/ease"

  /** A module's part of `ease.d.ts`. */
  const val TYPES_FILE = "types.d.ts"

  /** Starts the block of a type the module declares, followed by its serial name. */
  const val DECLARED_MARKER = "// ease-declared: "

  /** Starts the block of a type the module only reaches, followed by its serial name. */
  const val REACHED_MARKER = "// ease-reached: "

  /** Where a module's part is on its classpath. */
  const val RESOURCE = "$DIRECTORY/$TYPES_FILE"

  private const val HEADER =
    "// Written by the game's build from its modules' types; changes here are lost.\n"

  /** One type's declaration, from a module that [declared] it or only reached it. */
  data class Block(val name: String, val declared: Boolean, val text: String)

  /** The `ease.d.ts` text for [parts], in classpath order. */
  fun assemble(parts: List<String>): String {
    val chosen = linkedMapOf<String, Block>()
    parts
      .flatMap { blocksOf(it) }
      .forEach { block ->
        val kept = chosen[block.name]
        if (kept == null || (block.declared && !kept.declared)) chosen[block.name] = block
      }
    return HEADER + "\n" + chosen.toSortedMap().values.joinToString("\n") { it.text }
  }

  /** The blocks of [part], each with its marker line and its trailing newline. */
  fun blocksOf(part: String): List<Block> {
    val blocks = mutableListOf<Pair<Block, StringBuilder>>()
    part.lines().forEach { line ->
      markerOf(line)?.let { blocks += it to StringBuilder() }
      blocks.lastOrNull()?.second?.appendLine(line)
    }
    return blocks.map { (block, text) -> block.copy(text = text.toString().trimEnd() + "\n") }
  }

  private fun markerOf(line: String): Block? =
    when {
      line.startsWith(DECLARED_MARKER) -> Block(line.removePrefix(DECLARED_MARKER), true, "")
      line.startsWith(REACHED_MARKER) -> Block(line.removePrefix(REACHED_MARKER), false, "")
      else -> null
    }
}
