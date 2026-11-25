package dev.chieppa.ease.cli.template

import dev.chieppa.ease.cli.CliFailure

/** Adds a generator's lines to a file the project already has. */
public object Insertions {
  private const val IMPORT = "import "

  /**
   * [content] with [insertion]'s text above its marker line, indented like the marker, and its
   * imports among the file's imports in sorted places. Text already in the file is not added again,
   * so running a generator twice changes nothing the second time. Fails when the file has no
   * marker.
   */
  public fun apply(content: String, insertion: Insertion, file: String): String {
    val lines = content.split("\n").toMutableList()
    val at = lines.indexOfFirst { it.trim() == insertion.marker }
    if (at < 0) throw CliFailure("$file has no ${insertion.marker} line to add to")
    val indent = lines[at].takeWhile { it == ' ' }
    val block = insertion.text.removeSuffix("\n").split("\n")
    if (!containsBlock(lines, block)) {
      lines.addAll(at, block.map { if (it.isEmpty()) it else indent + it })
    }
    insertion.imports.forEach { addImport(lines, it) }
    return lines.joinToString("\n")
  }

  private fun containsBlock(lines: List<String>, block: List<String>): Boolean {
    val wanted = block.map { it.trim() }.filter { it.isNotEmpty() }
    val present = lines.map { it.trim() }
    return wanted.isNotEmpty() && wanted.all { it in present }
  }

  private fun addImport(lines: MutableList<String>, name: String) {
    val line = IMPORT + name
    if (line in lines) return
    val imports = lines.indices.filter { lines[it].startsWith(IMPORT) }
    when {
      imports.isEmpty() -> addFirstImport(lines, line)
      else -> {
        val after = imports.firstOrNull { lines[it] > line }
        lines.add(after ?: (imports.last() + 1), line)
      }
    }
  }

  private fun addFirstImport(lines: MutableList<String>, line: String) {
    val declaration = lines.indexOfFirst { it.startsWith("package ") }
    if (declaration < 0) throw CliFailure("Cannot add $line: the file has no package line")
    lines.addAll(declaration + 1, listOf("", line))
  }
}
