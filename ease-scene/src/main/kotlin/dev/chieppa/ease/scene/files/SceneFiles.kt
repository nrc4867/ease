package dev.chieppa.ease.scene.files

import dev.chieppa.ease.scene.document.SceneDocument
import dev.chieppa.ease.scene.document.SceneNode
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
  /**
   * The files [document] is written as: the header and one file per node, named as [NodeFileNames]
   * says. [previous] holds the file names (without folder or extension) the nodes had, so a node
   * that keeps its name keeps its file.
   */
  public fun encode(
    document: SceneDocument,
    previous: Map<LocalId, String> = emptyMap(),
  ): SortedMap<String, String> {
    val files = sortedMapOf(SceneFormat.HEADER_FILE to header(document))
    val names = NodeFileNames.assign(document, previous)
    document.nodes.values.forEach { node ->
      files[SceneFormat.nodeFile(names.getValue(node.id))] =
        CanonicalYaml.write(NodeFileFormat.nodeTree(node))
    }
    return files
  }

  /** The text of the header file for [document]. */
  public fun header(document: SceneDocument): String =
    CanonicalYaml.write(NodeFileFormat.headerTree(document.settings))

  /**
   * The document [files] hold, each file migrated to the current format in memory. Nodes are known
   * by the id inside their file, whatever the file is called. Files other than the header and the
   * node files are left alone. Throws [SceneFileException], naming the file, when one cannot be
   * read, or naming both files when two hold the same id.
   */
  public fun decode(files: Map<String, String>): SceneDocument {
    val headerText =
      files[SceneFormat.HEADER_FILE] ?: throw SceneFileException(SceneFormat.HEADER_FILE, "missing")
    val header = parse(SceneFormat.HEADER_FILE, headerText, SceneFormat.headerMigrations)
    val nodes = LinkedHashMap<LocalId, Pair<String, SceneNode>>()
    files
      .filterKeys { isNodeFile(it) }
      .forEach { (path, text) ->
        val tree = parse(path, text, SceneFormat.nodeMigrations)
        val node = reading(path) { NodeFileFormat.node(tree) }
        nodes.put(node.id, path to node)?.let { (other, _) ->
          throw SceneFileException(path, "holds node ${node.id}, as $other does")
        }
      }
    return SceneDocument(NodeFileFormat.settings(header), nodes.values.map { it.second })
  }

  /**
   * The file name, without folder or extension, of each node the node files among [files] hold;
   * files that cannot be read are left out.
   */
  public fun namesById(files: Map<String, String>): Map<LocalId, String> =
    files
      .filterKeys { isNodeFile(it) }
      .mapNotNull { (path, text) ->
        val node = runCatching {
          NodeFileFormat.node(parse(path, text, SceneFormat.nodeMigrations))
        }
          .getOrNull()
        node?.let { it.id to stemOf(path) }
      }
      .toMap()

  /** Whether [path] inside a scene folder is a node file. */
  public fun isNodeFile(path: String): Boolean =
    path.startsWith(SceneFormat.NODES_FOLDER + "/") &&
      path.endsWith(SceneFormat.NODE_EXTENSION) &&
      path.count { it == '/' } == 1

  private fun stemOf(path: String): String =
    path.removePrefix(SceneFormat.NODES_FOLDER + "/").removeSuffix(SceneFormat.NODE_EXTENSION)

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
