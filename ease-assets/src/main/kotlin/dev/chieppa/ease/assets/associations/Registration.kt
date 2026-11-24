package dev.chieppa.ease.assets.associations

/**
 * A contributor's place in the registry. Closing it removes the contributor, as unloading a game
 * module or plugin does; closing twice does nothing.
 */
public interface Registration : AutoCloseable {
  /** Whether the contributor is still registered. */
  public val isOpen: Boolean

  override fun close()
}
