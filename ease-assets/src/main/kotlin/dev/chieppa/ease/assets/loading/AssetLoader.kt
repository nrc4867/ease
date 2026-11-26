package dev.chieppa.ease.assets.loading

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.associations.Cardinality
import dev.chieppa.ease.assets.associations.Contributor
import dev.chieppa.ease.assets.associations.ContributorKind
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass

/**
 * Turns an asset's file bytes into the object games use. Loaders register with the file association
 * service under [AssetLoader.kind], so an asset's loader is whichever one is most specific for its
 * file, or for a cooked asset, for the cooked file's name.
 */
public interface AssetLoader<T : Any> : Contributor {
  /** What this loader produces. */
  public val type: KClass<T>

  /** The object [bytes] describe, for the asset [guid]. Throws when the bytes are not valid. */
  public fun load(guid: Guid, bytes: ByteArray): T

  /**
   * The object the cooked file at [path] holds, for the asset [guid]. A loader that maps its file
   * overrides this; by default it reads the file and calls the other [load].
   */
  public fun load(guid: Guid, path: Path): T = load(guid, Files.readAllBytes(path))

  public companion object {
    /** The contributor kind of loaders: one per file. */
    public val kind: ContributorKind<AssetLoader<*>> =
      ContributorKind("Loader", AssetLoader::class, Cardinality.ONE)
  }
}
