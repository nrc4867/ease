package dev.chieppa.ease.assets.folders

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.util.SortedMap

/** The bytes every pack starts with: `EPAK`. */
private const val MAGIC = 0x4550414b

/** The pack layout this Ease writes. */
private const val PACK_VERSION = 1

private const val HIDDEN_PREFIX = "."
private const val SEPARATOR = "/"

/**
 * A folder asset's files as one byte array, the same bytes for the same files on every machine:
 * each file under the folder (dot files and dot folders left out), sorted by its path inside the
 * folder written with `/`, then its bytes. A folder asset's content hash is the hash of its pack,
 * loaders read the pack, and cooking can write it as the asset's one file.
 */
public object FolderPack {
  /** The pack of the files under [folder]. */
  public fun pack(folder: Path): ByteArray {
    val files = sortedMapOf<String, ByteArray>()
    Files.walk(folder).use { walk ->
      walk
        .filter { Files.isRegularFile(it) }
        .map { folder.relativize(it) }
        .filter { relative -> relative.none { it.toString().startsWith(HIDDEN_PREFIX) } }
        .forEach { files[relative(it)] = Files.readAllBytes(folder.resolve(it)) }
    }
    return pack(files)
  }

  /** The pack of [files], keyed by their path inside the folder written with `/`. */
  public fun pack(files: Map<String, ByteArray>): ByteArray {
    val bytes = ByteArrayOutputStream()
    DataOutputStream(bytes).use { out ->
      out.writeInt(MAGIC)
      out.writeInt(PACK_VERSION)
      out.writeInt(files.size)
      files.toSortedMap().forEach { (path, contents) ->
        val name = path.toByteArray(Charsets.UTF_8)
        out.writeInt(name.size)
        out.write(name)
        out.writeInt(contents.size)
        out.write(contents)
      }
    }
    return bytes.toByteArray()
  }

  /**
   * The files [pack] holds, by their path inside the folder. Throws [IllegalArgumentException] when
   * the bytes are not a pack.
   */
  public fun unpack(pack: ByteArray): SortedMap<String, ByteArray> {
    val buffer = ByteBuffer.wrap(pack)
    try {
      require(buffer.getInt() == MAGIC) { "Not a folder pack" }
      require(buffer.getInt() == PACK_VERSION) { "Unknown folder pack version" }
      val count = buffer.getInt()
      require(count >= 0) { "Negative file count" }
      val files = sortedMapOf<String, ByteArray>()
      repeat(count) {
        val name = String(bytes(buffer), Charsets.UTF_8)
        files[name] = bytes(buffer)
      }
      require(!buffer.hasRemaining()) { "Bytes after the last file" }
      return files
    } catch (short: BufferUnderflowException) {
      throw IllegalArgumentException("The folder pack is cut short", short)
    }
  }

  private fun bytes(buffer: ByteBuffer): ByteArray {
    val length = buffer.getInt()
    require(length in 0..buffer.remaining()) { "A length of $length past the end of the pack" }
    return ByteArray(length).also { buffer.get(it) }
  }

  private fun relative(path: Path): String = path.joinToString(SEPARATOR)
}
