package dev.chieppa.ease.memory

import java.lang.foreign.Arena
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * A file mapped read-only into memory, read through little-endian views: [whole] covers the file
 * and [region] a part of it. Closing it unmaps the file, after which reads fail.
 */
public class MappedFile private constructor(public val path: Path, private val arena: Arena) :
  AutoCloseable {
  /** The whole file. */
  public val whole: MappedRegion =
    FileChannel.open(path, StandardOpenOption.READ).use { channel ->
      val segment = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size(), arena)
      MappedRegion(segment, path.toString(), 0L)
    }

  /** The file's length in bytes. */
  public val byteSize: Long
    get() = whole.byteSize

  /** The [length] bytes from [offset]; fails when they run past the file's end. */
  public fun region(offset: Long, length: Long): MappedRegion = whole.region(offset, length)

  override fun close() {
    arena.close()
  }

  public companion object {
    /** Maps the file at [path], read-only. */
    public fun open(path: Path): MappedFile = MappedFile(path, Arena.ofShared())
  }
}
