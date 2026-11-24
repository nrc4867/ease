package dev.chieppa.ease.scene.files

import dev.chieppa.ease.scene.document.SceneDocument
import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.serialization.format.FormatMigrations
import dev.chieppa.ease.serialization.format.UnsupportedFormatVersion
import dev.chieppa.ease.serialization.yaml.CanonicalYaml
import dev.chieppa.ease.serialization.yaml.YamlFormatException
import java.util.SortedMap
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject

/**
 * A scene or prefab as the text of its files, by path inside its folder written with `/`. Every
 * file is canonical YAML, so the same document always gives the same bytes and an edit to one node
 * changes only that node's file.
 */
public object SceneFiles {
  /** The files [document] is written as: the header and one file per node. */
  public fun encode(document: SceneDocument): SortedMap<String, String> {
    val files = sortedMapOf(SceneFormat.HEADER_FILE to header(document))
    document.nodes.values.forEach { node ->
      files[SceneFormat.nodeFile(node.id.text)] = CanonicalYaml.write(NodeFileFormat.nodeTree(node))
    }
    return files
  }

  /** The text of the header file for [document]. */
  public fun header(document: SceneDocument): String =
    CanonicalYaml.write(NodeFileFormat.headerTree(document.settings))

  /**
   * The document [files] hold, each file migrated to the current format in memory. Files other than
   * the header and the node files are left alone. Throws [SceneFileException], naming the file,
   * when one cannot be read.
   */
  public fun decode(files: Map<String, String>): SceneDocument {
    val headerText =
      files[SceneFormat.HEADER_FILE] ?: throw SceneFileException(SceneFormat.HEADER_FILE, "missing")
    val header = parse(SceneFormat.HEADER_FILE, headerText, SceneFormat.headerMigrations)
    val nodes =
      files
        .filterKeys { isNodeFile(it) }
        .map { (path, text) ->
          val tree = parse(path, text, SceneFormat.nodeMigrations)
          reading(path) { NodeFileFormat.node(idOf(path), tree) }
        }
    return SceneDocument(NodeFileFormat.settings(header), nodes)
  }

  /** Whether [path] inside a scene folder is a node file. */
  public fun isNodeFile(path: String): Boolean =
    path.startsWith(SceneFormat.NODES_FOLDER + "/") &&
      path.endsWith(SceneFormat.NODE_EXTENSION) &&
      path.count { it == '/' } == 1

  private fun idOf(path: String): LocalId =
    LocalId.parse(
      path.removePrefix(SceneFormat.NODES_FOLDER + "/").removeSuffix(SceneFormat.NODE_EXTENSION)
    )

  private fun parse(path: String, text: String, migrations: FormatMigrations): JsonObject =
    reading(path) {
      val tree =
        CanonicalYaml.read(text) as? JsonObject ?: throw YamlFormatException("not a mapping")
      migrations.migrate(tree)
    }

  private inline fun <T> reading(path: String, read: () -> T): T {
    val failure: Exception =
      try {
        return read()
      } catch (failure: YamlFormatException) {
        failure
      } catch (failure: UnsupportedFormatVersion) {
        failure
      } catch (failure: SerializationException) {
        failure
      } catch (failure: IllegalArgumentException) {
        failure
      }
    throw SceneFileException(path, failure.message.orEmpty(), failure)
  }
}
