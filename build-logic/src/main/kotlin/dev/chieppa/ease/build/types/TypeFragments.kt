package dev.chieppa.ease.build.types

/**
 * Joins the TypeScript parts every module's `ease-codegen` run writes into one `ease.d.ts`. A part
 * is a list of blocks, each starting with a [BLOCK_MARKER] line naming its type; a type that
 * several parts declare is kept once, and the blocks come out in type-name order.
 */
object TypeFragments {
  /** The line that starts each block, followed by the type's serial name. */
  const val BLOCK_MARKER = "// ease-type: "

  /** Where a module's part is on its classpath. */
  const val RESOURCE = "META-INF/ease/types.d.ts"

  private const val HEADER =
    "// Written by the game's build from its modules' types; changes here are lost.\n"

  /** The `ease.d.ts` text for [parts], in classpath order; the first block of a type wins. */
  fun assemble(parts: List<String>): String {
    val blocks = linkedMapOf<String, String>()
    parts.forEach { part ->
      blocksOf(part).forEach { (name, block) -> blocks.putIfAbsent(name, block) }
    }
    return HEADER + "\n" + blocks.toSortedMap().values.joinToString("\n")
  }

  /** The blocks of [part] by type name, each with its marker line and its trailing newline. */
  fun blocksOf(part: String): List<Pair<String, String>> {
    val blocks = mutableListOf<Pair<String, StringBuilder>>()
    part.lines().forEach { line ->
      if (line.startsWith(BLOCK_MARKER)) {
        blocks += line.removePrefix(BLOCK_MARKER) to StringBuilder()
      }
      blocks.lastOrNull()?.second?.appendLine(line)
    }
    return blocks.map { (name, text) -> name to text.toString().trimEnd() + "\n" }
  }
}
