package dev.chieppa.ease.cli.project

import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.cli.CliFailure
import dev.chieppa.ease.cli.template.Naming
import dev.chieppa.ease.serialization.format.FormatMigrations
import dev.chieppa.ease.serialization.yaml.CanonicalYaml
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

@Doc("A game project's manifest: what generators need to know to add to it.")
@Serializable
public class ProjectFile(
  @Doc("The game's name, as a person writes it.") public val name: String,
  @Doc("The package the game's types live in.")
  @SerialName("package")
  public val packageName: String,
  @Doc("The name of the game's `GameModule` class.") public val className: String,
  @Doc("The folder of the module that holds the game's `GameModule`.") public val module: String,
  @Doc("The exact Ease version the game builds against.") public val easeVersion: String,
  @Doc("The template the game was made from.") public val template: String,
) {
  @Doc("The version of this file's format.") public val formatVersion: Int = FORMAT_VERSION

  /** The values generators fill their templates with, from this project. */
  public fun placeholders(): Map<String, String> =
    mapOf(
      "projectName" to name,
      "projectId" to Naming.kebab(name),
      "serialPrefix" to Naming.flat(name),
      "package" to packageName,
      "packagePath" to packageName.replace('.', '/'),
      "className" to className,
      "gameModuleDir" to module,
      "easeVersion" to easeVersion,
    )

  /** Reading and writing the file. */
  public companion object {
    /** The current format of the project file. */
    public const val FORMAT_VERSION: Int = 1

    /** The file at the root of every game project. */
    public const val FILE_NAME: String = "ease-project.yaml"

    private val migrations = FormatMigrations(FORMAT_VERSION)

    /** The project file in [directory]. Fails when there is none, as when not in a game. */
    public fun read(directory: Path): ProjectFile {
      val file = directory.resolve(FILE_NAME)
      if (!file.exists()) throw CliFailure("$FILE_NAME not found in $directory: not a game project")
      val tree =
        CanonicalYaml.read(file.readText()) as? JsonObject
          ?: throw CliFailure("$FILE_NAME is not a mapping")
      return Json.decodeFromJsonElement(serializer(), migrations.migrate(tree))
    }

    /** Writes [project] as `ease-project.yaml` in [directory]. */
    public fun write(directory: Path, project: ProjectFile) {
      directory.resolve(FILE_NAME).writeText(CanonicalYaml.encode(serializer(), project))
    }
  }
}
