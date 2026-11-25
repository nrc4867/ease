package dev.chieppa.ease.cli.template

import dev.chieppa.ease.cli.CliFailure
import dev.chieppa.ease.serialization.format.FormatMigrations
import dev.chieppa.ease.serialization.yaml.CanonicalYaml
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/** Templates read from folders under [root]: `projects`, `generators` and `parts`. */
internal class DirectoryTemplates(private val root: Path) : TemplateSource {
  override fun projects(): List<String> = ids(PROJECTS)

  override fun generators(): List<String> = ids(GENERATORS)

  override fun project(id: String): Template = load(PROJECTS, id)

  override fun generator(id: String): Template = load(GENERATORS, id)

  private fun ids(kind: String): List<String> {
    val folder = root.resolve(kind)
    if (!folder.isDirectory()) return emptyList()
    val entries = Files.list(folder).use { it.toList() }
    return entries.filter { it.isDirectory() }.map { it.name }.sorted()
  }

  private fun load(kind: String, id: String): Template {
    val folder = root.resolve(kind).resolve(id)
    if (!folder.resolve(TemplateManifest.FILE_NAME).exists()) {
      throw CliFailure("No $kind template '$id'; there are: ${ids(kind).joinToString()}")
    }
    val manifest = readManifest(folder.resolve(TemplateManifest.FILE_NAME))
    val parts = manifest.parts.flatMap { part -> filesOf(root.resolve(PARTS).resolve(part)) }
    return Template(id, manifest, parts + filesOf(folder.resolve(FILES)))
  }

  private fun readManifest(file: Path): TemplateManifest {
    val tree =
      CanonicalYaml.read(file.readText()) as? JsonObject
        ?: throw CliFailure("${file.name} is not a mapping")
    val current = FormatMigrations(TemplateManifest.FORMAT_VERSION).migrate(tree)
    return Json.decodeFromJsonElement(TemplateManifest.serializer(), current)
  }

  private fun filesOf(folder: Path): List<TemplateFile> {
    if (!folder.isDirectory()) throw CliFailure("Missing template folder ${folder.name}")
    return Files.walk(folder).use { paths ->
      paths
        .filter { !it.isDirectory() }
        .map { TemplateFile(folder.relativize(it).joinToString("/"), it.readBytes()) }
        .toList()
        .sortedBy { it.path }
    }
  }

  private companion object {
    const val PROJECTS = "projects"
    const val GENERATORS = "generators"
    const val PARTS = "parts"
    const val FILES = "files"
  }
}
