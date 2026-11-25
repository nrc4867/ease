package dev.chieppa.ease.scene.document

import dev.chieppa.ease.assets.Guid
import java.util.SortedMap
import kotlinx.serialization.json.JsonElement

/**
 * What makes a node a placement of another scene or prefab: the [asset] placed, a prefab or a
 * sublevel, and the [overrides] laid over it, sorted by path. The placed asset's top nodes become
 * the node's children. When the asset changes, every instance follows, and the overrides are laid
 * over the new contents again.
 */
public class Instance(
  public val asset: Guid,
  overrides: Map<OverridePath, JsonElement> = emptyMap(),
) {
  /** The overrides by path, in path order. */
  public val overrides: SortedMap<OverridePath, JsonElement> = overrides.toSortedMap()

  /** This instance with [value] at [path], replacing any override there. */
  public fun withOverride(path: OverridePath, value: JsonElement): Instance =
    Instance(asset, overrides + (path to value))

  /** This instance without the override at [path]. */
  public fun withoutOverride(path: OverridePath): Instance = Instance(asset, overrides - path)

  override fun equals(other: Any?): Boolean =
    other is Instance && other.asset == asset && other.overrides == overrides

  override fun hashCode(): Int = asset.hashCode() * HASH_PRIME + overrides.hashCode()

  private companion object {
    const val HASH_PRIME = 31
  }
}
