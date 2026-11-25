package dev.chieppa.ease.cli.template

import dev.chieppa.ease.cli.CliFailure
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

/**
 * What generating [template] into a folder would write: new files, and edits to files already
 * there. Built whole before anything is written, so a generator that cannot finish changes nothing.
 */
public class GenerationPlan
private constructor(
  private val writes: Map<Path, ByteArray>,
  private val edits: Map<Path, String>,
) {
  /** The files this plan creates. */
  public val created: Set<Path>
    get() = writes.keys

  /** The files this plan changes. */
  public val changed: Set<Path>
    get() = edits.keys

  /** Writes every file and edit. */
  public fun apply() {
    writes.forEach { (path, bytes) ->
      path.parent.createDirectories()
      path.writeBytes(bytes)
      if (path.fileName.toString() in EXECUTABLES) path.toFile().setExecutable(true)
    }
    edits.forEach { (path, content) -> path.writeText(content) }
  }

  /** Building plans. */
  public companion object {
    private val EXECUTABLES = setOf("gradlew")

    /**
     * The plan for [template] in [target] with [placeholders]. Fails when a file it creates is
     * already there, or an insertion has nowhere to go.
     */
    public fun of(template: Template, placeholders: Placeholders, target: Path): GenerationPlan {
      val writes = linkedMapOf<Path, ByteArray>()
      for (file in template.files) {
        val path = target.resolve(placeholders.render(file.targetPath)).normalize()
        if (!path.startsWith(target.normalize()))
          throw CliFailure("${file.path} leaves the project")
        if (path.exists()) throw CliFailure("${target.relativize(path)} already exists")
        writes[path] =
          if (file.isText) placeholders.render(file.bytes.decodeToString()).encodeToByteArray()
          else file.bytes
      }
      return GenerationPlan(writes, edits(template, placeholders, target))
    }

    private fun edits(
      template: Template,
      placeholders: Placeholders,
      target: Path,
    ): Map<Path, String> {
      val edits = linkedMapOf<Path, String>()
      for (insertion in template.manifest.insertions) {
        val file = placeholders.render(insertion.file)
        val path = target.resolve(file).normalize()
        if (!Files.exists(path)) throw CliFailure("$file not found: nothing to add to")
        val rendered =
          Insertion(
            file,
            insertion.marker,
            placeholders.render(insertion.text),
            insertion.imports.map(placeholders::render),
          )
        edits[path] = Insertions.apply(edits[path] ?: path.readText(), rendered, file)
      }
      return edits
    }
  }
}
