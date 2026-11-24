package dev.chieppa.ease.assets.database

import dev.chieppa.ease.assets.ContentHash
import dev.chieppa.ease.assets.associations.FileAssociations
import dev.chieppa.ease.assets.associations.FolderAsset
import dev.chieppa.ease.assets.associations.ProjectFile
import dev.chieppa.ease.assets.folders.FolderPack
import dev.chieppa.ease.assets.meta.MetaFiles
import java.nio.file.Files
import java.nio.file.Path

/**
 * What counts as an asset's source under [root]: a file, or a folder that a [FolderAsset]
 * contributor in [associations] names, such as `cave.scene/`. A file inside such a folder belongs
 * to the folder's asset, so it needs no meta file of its own. Paths are relative to [root].
 */
internal class SourceFiles(private val root: Path, private val associations: FileAssociations) {
  /** Whether the folder at [relative] is one asset, by its name alone. */
  fun isFolderAsset(relative: Path): Boolean =
    associations.resolve(FolderAsset.kind, ProjectFile.under(root, relative)).isNotEmpty()

  /**
   * The source the file at [relative] belongs to: itself, the source a meta file describes, or the
   * outermost folder asset it sits in.
   */
  fun sourceOf(relative: Path): Path {
    val path = if (MetaFiles.isMeta(relative)) MetaFiles.sourceOf(relative) else relative
    for (depth in 1 until path.nameCount) {
      val folder = path.subpath(0, depth)
      if (isFolderAsset(folder)) return folder
    }
    return path
  }

  /** Whether [source] is on disk: a file, or a folder asset's folder. */
  fun exists(source: Path): Boolean {
    val absolute = root.resolve(source)
    return Files.isRegularFile(absolute) || Files.isDirectory(absolute) && isFolderAsset(source)
  }

  /** The bytes of [source]: a file's contents, or a folder asset's [FolderPack]. */
  fun read(source: Path): ByteArray {
    val absolute = root.resolve(source)
    return if (Files.isDirectory(absolute)) FolderPack.pack(absolute)
    else Files.readAllBytes(absolute)
  }

  /** The content hash of [source], from the same bytes [read] gives. */
  fun hash(source: Path): String = ContentHash.of(read(source))
}
