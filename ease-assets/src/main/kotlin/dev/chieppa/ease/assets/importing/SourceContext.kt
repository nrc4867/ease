package dev.chieppa.ease.assets.importing

import dev.chieppa.ease.assets.ContentHash
import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.memory.MappedFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.util.TreeMap
import java.util.TreeSet

/**
 * The [ImportContext] of one import of the source at [source] (relative to [root]): it reads files
 * beside the source, records each one's hash in [inputs] by its path from [root], and unmaps what
 * it mapped on [close].
 */
internal class SourceContext(
  private val root: Path,
  private val source: Path,
  override val guid: Guid,
) : ImportContext, AutoCloseable {
  /** Each file read, by its path from the content folder with `/`, and its contents hash. */
  val inputs: TreeMap<String, String> = TreeMap()

  private val mapped = mutableListOf<MappedFile>()

  override val sourceName: String = source.fileName.toString()

  /** Each file the importer asked for, whether or not it could be read. */
  val attempted: TreeSet<String> = TreeSet()

  override fun read(relativePath: String): ByteArray {
    val (key, file) = resolve(relativePath)
    attempted += key
    val bytes =
      try {
        Files.readAllBytes(file)
      } catch (failure: IOException) {
        throw unreadable(relativePath, failure)
      }
    inputs[key] = ContentHash.of(bytes)
    return bytes
  }

  override fun map(relativePath: String): MappedFile {
    val (key, file) = resolve(relativePath)
    attempted += key
    return try {
      inputs[key] = ContentHash.of(file)
      MappedFile.open(file).also { mapped += it }
    } catch (failure: IOException) {
      throw unreadable(relativePath, failure)
    }
  }

  private fun unreadable(relativePath: String, failure: IOException) =
    ImportException("cannot read '$relativePath' beside $sourceName: ${failure.message}", failure)

  override fun close() {
    mapped.forEach(MappedFile::close)
    mapped.clear()
  }

  private fun resolve(relativePath: String): Pair<String, Path> {
    val folder = root.resolve(source).parent ?: root
    val file =
      try {
        folder.resolve(relativePath).normalize()
      } catch (failure: InvalidPathException) {
        throw ImportException("'$relativePath' beside $sourceName is not a path", failure)
      }
    val relative = if (file.startsWith(root) && file != root) root.relativize(file) else null
    val problem =
      when {
        relative == null -> "is outside the content folder"
        relative.any { it.toString().startsWith(".") } -> "is in a hidden folder"
        else -> null
      }
    if (relative == null || problem != null) {
      throw ImportException("'$relativePath' beside $sourceName $problem")
    }
    return relative.joinToString("/") to file
  }

  companion object {
    /** The hashes of [inputs] as they are on disk now under [root]; a missing file hashes empty. */
    fun hashesNow(root: Path, inputs: Collection<String>): Map<String, String> =
      inputs.associateWith { path ->
        val file = root.resolve(path)
        if (Files.isRegularFile(file)) ContentHash.of(file) else ""
      }
  }
}
