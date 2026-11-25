package dev.chieppa.ease.assets.associations

import java.util.ServiceLoader

/**
 * A module that contributes to the file association service: a game, an Ease module or a plugin. It
 * is listed in `META-INF/services/dev.chieppa.ease.assets.associations.ContributorProvider`, and a
 * tool such as `ease import` finds every provider on its classpath with [loadAll] and asks each to
 * register. The editor and headless tools call it; the player has no registry.
 */
public interface ContributorProvider {
  /**
   * Registers the module's importers and other contributors with [associations]. Everything
   * registered through it is removed together when the module is unloaded.
   */
  public fun registerContributors(associations: FileAssociations) {}

  public companion object {
    /** Every provider [loader] lists as a service. */
    public fun loadAll(
      loader: ClassLoader = checkNotNull(ContributorProvider::class.java.classLoader)
    ): List<ContributorProvider> =
      ServiceLoader.load(ContributorProvider::class.java, loader).toList()
  }
}
