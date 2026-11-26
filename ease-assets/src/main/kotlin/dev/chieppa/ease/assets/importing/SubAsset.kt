package dev.chieppa.ease.assets.importing

import dev.chieppa.ease.assets.Guid

/** A cooked asset known by its own GUID: the cooked asset [name] of the source [parent]. */
public class SubAsset(public val parent: Guid, public val name: String) {
  /** The sub-asset's GUID: the parent's named by [name]. */
  public val guid: Guid = parent.named(name)

  override fun equals(other: Any?): Boolean =
    other is SubAsset && other.parent == parent && other.name == name

  override fun hashCode(): Int = parent.hashCode() * HASH_PRIME + name.hashCode()

  override fun toString(): String = "SubAsset($parent/$name)"

  private companion object {
    const val HASH_PRIME = 31
  }
}
