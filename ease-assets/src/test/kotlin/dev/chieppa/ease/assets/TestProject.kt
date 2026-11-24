package dev.chieppa.ease.assets

import dev.chieppa.ease.assets.meta.AssetMeta
import dev.chieppa.ease.assets.meta.MetaFiles
import java.nio.file.Files
import java.nio.file.Path

/** A project folder on disk for tests: files written with or without their meta files. */
internal class TestProject(val root: Path) {
  private var counter = 0L

  /** GUIDs 0-0-0-0-1, 2, 3, and so on, in order. */
  val guids = GuidSource { Guid(0, ++counter) }

  /** Writes [text] to [name] under the project, creating folders. */
  fun write(name: String, text: String): Path {
    val file = root.resolve(name)
    Files.createDirectories(file.parent)
    Files.writeString(file, text)
    return file
  }

  /** Writes a source file and its meta file, with the GUID [guid] unless one is given. */
  fun asset(name: String, text: String, guid: Guid = guids.next()): Guid {
    val file = write(name, text)
    MetaFiles.write(
      MetaFiles.metaOf(file),
      AssetMeta(guid = guid, contentHash = ContentHash.of(file)),
    )
    return guid
  }

  fun path(name: String): Path = root.resolve(name)
}
