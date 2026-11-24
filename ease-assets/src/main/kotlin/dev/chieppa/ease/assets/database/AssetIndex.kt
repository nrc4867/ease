package dev.chieppa.ease.assets.database

import dev.chieppa.ease.assets.Guid
import java.nio.file.Path

/** The records by GUID and by path, kept in step. */
internal class AssetIndex {
  private val byGuid = HashMap<Guid, AssetRecord>()
  private val byPath = HashMap<Path, AssetRecord>()

  val records: Collection<AssetRecord>
    get() = byGuid.values

  operator fun get(guid: Guid): AssetRecord? = byGuid[guid]

  fun at(path: Path): AssetRecord? = byPath[path]

  fun put(record: AssetRecord) {
    byGuid[record.guid] = record
    byPath[record.path] = record
  }

  fun remove(record: AssetRecord) {
    byGuid.remove(record.guid)
    byPath.remove(record.path)
  }

  /** The records whose path is [folder] or under it. */
  fun under(folder: Path): List<AssetRecord> = byPath.values.filter { it.path.startsWith(folder) }
}
