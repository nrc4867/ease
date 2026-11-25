package dev.chieppa.ease.cli.template

import dev.chieppa.ease.cli.CliFailure

/** Adds a generator's lines to a file the project already has. */
public object Insertions {
  private const val IMPORT = "import "

  /**
   * [content] with [insertion]'s text above its marker line, indented like the marker, and its
   * imports among the file's imports in sorted places. Text already directly above the marker, as
   * the same consecutive lines, is not added again, so running a generator twice changes nothing
   * the second time. Fails when the file has no marker.
   */
  public fun apply(content: String, insertion: Insertion, file: String): String {
    val lines = content.split("\n").toMutableList()
    val at = lines.indexOfFirst { it.trim() == insertion.marker }
    if (at < 0) throw CliFailure("$file has no ${insertion.marker} line to add to")
    val indent = lines[at].takeWhile { it == ' ' }
    val block = insertion.text.removeSuffix("\n").split("\n")
    if (!presentAbove(lines, at, block)) {
      lines.addAll(at, block.map { if (it.isEmpty()) it else indent + it })
    }
    insertion.imports.forEach { addImport(lines, it) }
    return lines.joinToString("\n")
  }

  /**
   * Whether [block] is already directly above index [marker] in [lines]: as the last lines before
   * it, or, for a block with no blank line, as consecutive lines among the statements stacked there
   * by earlier insertions (the lines up to the previous blank line or opening brace).
   */
  private fun presentAbove(lines: List<String>, marker: Int, block: List<String>): Boolean {
    val wanted = block.map { it.trim() }
    val above = lines.subList(0, marker).map { it.trim() }
    return above.takeLast(wanted.size) == wanted ||
      (wanted.none { it.isEmpty() } && containsRun(stackedAbove(above), wanted))
  }

  /** The statements directly above a marker: up to the previous blank line or opening brace. */
  private fun stackedAbove(above: List<String>): List<String> = above.takeLastWhile {
    it.isNotEmpty() && !it.endsWith("{")
  }

  private fun containsRun(lines: List<String>, run: List<String>): Boolean =
    (0..lines.size - run.size).any { lines.subList(it, it + run.size) == run }

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
