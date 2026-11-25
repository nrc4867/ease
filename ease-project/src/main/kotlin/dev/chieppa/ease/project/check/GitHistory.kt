package dev.chieppa.ease.project.check

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.meta.MetaFiles
import dev.chieppa.ease.project.git.Git
import dev.chieppa.ease.project.git.GitFailure

/**
 * Where git's history last had an asset: its sidecar, the commit and who made it. [meta] is the
 * sidecar's path from the repository root, written with `/`.
 */
public class AssetSighting(
  /** The commit that last added, changed or deleted the sidecar. */
  public val commit: String,
  /** Who made the commit. */
  public val author: String,
  /** The sidecar's path from the repository root. */
  public val meta: String,
  /** Whether that commit deleted it. */
  public val deleted: Boolean,
) {
  /** The commit's first characters, as people quote it. */
  public val shortCommit: String
    get() = commit.take(SHORT)

  private companion object {
    const val SHORT = 8
  }
}

/** Looks an asset's GUID up in git's history, for dangling references. */
public class GitHistory(private val git: Git) {
  private val cache = HashMap<Guid, AssetSighting?>()

  /**
   * The newest commit that touched a sidecar holding [guid], or null when none did. Newest means
   * children before parents, so a deletion is found before the addition it undoes whatever the
   * commit dates say.
   */
  public fun sighting(guid: Guid): AssetSighting? = cache.getOrPut(guid) { search(guid) }

  private fun search(guid: Guid): AssetSighting? =
    try {
      val log =
        git.run(
          "log",
          "--all",
          "--topo-order",
          "--no-renames",
          "-z",
          "-S$guid",
          "--format=%x01%H%x00%an",
          "--name-status",
          "--",
          "*${MetaFiles.SUFFIX}",
        )
      if (log.succeeded)
        entries(log.nulFields).take(MAX_COMMITS).firstNotNullOfOrNull {
          sighting(it, guid)
        }
      else null
    } catch (_: GitFailure) {
      null
    }

  /** One commit of the log with the sidecars it changed, as git's status letter and path. */
  private class Entry(val commit: String, val author: String) {
    val changes = mutableListOf<Pair<String, String>>()
  }

  /**
   * The commits of `log -z` output: a field starting with [START] opens one, followed by its
   * author, then pairs of a status letter and a path.
   */
  private fun entries(fields: List<String>): List<Entry> {
    val entries = mutableListOf<Entry>()
    var index = 0
    while (index < fields.size) {
      val field = fields[index]
      if (field.startsWith(START)) {
        entries += Entry(field.removePrefix(START), fields.getOrElse(index + 1) { "" })
        index += HEADER_FIELDS
      } else {
        val path = fields.getOrNull(index + 1)
        if (path != null) entries.lastOrNull()?.changes?.add(field.trim() to path)
        index += CHANGE_FIELDS
      }
    }
    return entries
  }

  private fun sighting(entry: Entry, guid: Guid): AssetSighting? =
    entry.changes.firstNotNullOfOrNull { (status, path) ->
      val deleted = status == DELETED
      val revision = if (deleted) "${entry.commit}^" else entry.commit
      AssetSighting(entry.commit, entry.author, path, deleted).takeIf {
        holds(revision, path, guid)
      }
    }

  private fun holds(revision: String, path: String, guid: Guid): Boolean =
    git.run("show", "$revision:$path").let { it.succeeded && it.output.contains(guid.toString()) }

  private companion object {
    const val START = "\u0001"
    const val HEADER_FIELDS = 2
    const val CHANGE_FIELDS = 2
    const val MAX_COMMITS = 40
    const val DELETED = "D"
  }
}
