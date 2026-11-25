package dev.chieppa.ease.sample

import dev.chieppa.ease.assets.associations.FileAssociations
import dev.chieppa.ease.assets.associations.FileMatch
import dev.chieppa.ease.assets.importing.CookedAsset
import dev.chieppa.ease.assets.importing.ImportException
import dev.chieppa.ease.assets.importing.Importer
import java.io.ByteArrayOutputStream
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * Imports the sample's `.palette` files: text with one `name rrggbb` color per line (blank lines
 * and lines starting with `#` are skipped). It cooks `colors.bin`, the color count as one byte and
 * then three bytes per color, and `names.txt`, the names one per line. The setting `sorted: true`
 * orders the colors by name.
 */
public object PaletteImporter : Importer {
  override val id: String = "palette"

  override val version: Int = 1

  private const val MAX_COLORS = 255
  private const val HEX = 16
  private const val HEX_DIGITS = 6
  private const val BLUE_SHIFT = 0
  private const val GREEN_SHIFT = 8
  private const val RED_SHIFT = 16
  private const val BYTE_MASK = 0xff

  /** Registers the importer for the `palette` extension. */
  public fun register(associations: FileAssociations): Unit =
    associations
      .register(Importer.kind, FileMatch.Extension("palette"), 0, PaletteImporter, "sample")
      .let {}

  override fun import(source: ByteArray, settings: JsonObject): List<CookedAsset> {
    val colors = parse(source.toString(Charsets.UTF_8))
    val sorted = (settings["sorted"] as? JsonPrimitive)?.booleanOrNull ?: false
    val ordered = if (sorted) colors.sortedBy { it.first } else colors
    val bytes = ByteArrayOutputStream()
    bytes.write(ordered.size)
    for ((_, rgb) in ordered) {
      bytes.write((rgb shr RED_SHIFT) and BYTE_MASK)
      bytes.write((rgb shr GREEN_SHIFT) and BYTE_MASK)
      bytes.write((rgb shr BLUE_SHIFT) and BYTE_MASK)
    }
    val names = ordered.joinToString("") { it.first + "\n" }
    return listOf(
      CookedAsset("colors.bin", bytes.toByteArray()),
      CookedAsset("names.txt", names.toByteArray(Charsets.UTF_8)),
    )
  }

  private fun parse(text: String): List<Pair<String, Int>> {
    val colors =
      text
        .lines()
        .withIndex()
        .map { it.index + 1 to it.value.trim() }
        .filter { it.second.isNotEmpty() && !it.second.startsWith("#") }
        .map { (line, content) -> parseLine(line, content) }
    if (colors.size > MAX_COLORS)
      throw ImportException("A palette holds at most $MAX_COLORS colors")
    return colors
  }

  private fun parseLine(line: Int, content: String): Pair<String, Int> {
    val parts = content.split(Regex("\\s+"))
    val value = parts.getOrNull(1)
    if (parts.size != 2 || value == null || value.length != HEX_DIGITS) {
      throw ImportException("Line $line: expected 'name rrggbb', got '$content'")
    }
    val rgb =
      value.toIntOrNull(HEX) ?: throw ImportException("Line $line: '$value' is not a hex color")
    return parts[0] to rgb
  }
}
