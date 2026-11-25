package dev.chieppa.ease.cli.template

import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.Serializable

@Doc("What a template is: its description, the shared parts it includes and what it edits.")
@Serializable
public class TemplateManifest(
  @Doc("The version of this file's format.") public val formatVersion: Int = FORMAT_VERSION,
  @Doc("One line saying what the template makes.") public val description: String,
  @Doc("The folder, in the generated project, that holds the game's module.")
  public val gameModule: String = DEFAULT_GAME_MODULE,
  @Doc("The shared parts copied in before the template's own files, in order.")
  public val parts: List<String> = emptyList(),
  @Doc("The lines a generator adds to files the project already has.")
  public val insertions: List<Insertion> = emptyList(),
) {
  /** The current format of a template manifest. */
  public companion object {
    /** The format version this build reads and writes. */
    public const val FORMAT_VERSION: Int = 1

    /** The file in a template folder that holds the manifest. */
    public const val FILE_NAME: String = "template.yaml"

    /** The folder a project's game module lives in unless the template says otherwise. */
    public const val DEFAULT_GAME_MODULE: String = "game"
  }
}
