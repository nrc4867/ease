package dev.chieppa.ease.scene.files

import dev.chieppa.ease.serialization.format.FormatMigrations

/**
 * The names and version of the scene format. A scene `cave.scene/` or a prefab `crate.prefab/` is a
 * folder holding [HEADER_FILE] and one file per node in [NODES_FOLDER], named by the node's local
 * id. Every file carries `formatVersion`; reading migrates an older file in memory one version at a
 * time, and only an explicit project upgrade writes the result.
 */
public object SceneFormat {
  /** The scene format version this Ease writes. */
  public const val FORMAT_VERSION: Int = 1

  /** The extension of a scene's folder. */
  public const val SCENE_EXTENSION: String = "scene"

  /** The extension of a prefab's folder. */
  public const val PREFAB_EXTENSION: String = "prefab"

  /** The file holding the format version and the scene-wide settings. */
  public const val HEADER_FILE: String = "scene.yaml"

  /** The folder holding one file per node. */
  public const val NODES_FOLDER: String = "nodes"

  /** The extension of a node file. */
  public const val NODE_EXTENSION: String = ".yaml"

  /** The steps that bring a header file to [FORMAT_VERSION]. */
  internal val headerMigrations: FormatMigrations = FormatMigrations(FORMAT_VERSION)

  /** The steps that bring a node file to [FORMAT_VERSION]. */
  internal val nodeMigrations: FormatMigrations = FormatMigrations(FORMAT_VERSION)

  /** The path of the node file for [id] inside the folder, written with `/`. */
  public fun nodeFile(id: String): String = "$NODES_FOLDER/$id$NODE_EXTENSION"
}
