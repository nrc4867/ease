package dev.chieppa.ease.runtime

import java.util.ServiceLoader

/** Finds the game a host runs: the one [GameModule] on its classpath. */
public object GameModules {
  /** Every [GameModule] [loader] lists as a service, for tools that work on any game. */
  public fun loadAll(
    loader: ClassLoader = checkNotNull(GameModules::class.java.classLoader)
  ): List<GameModule> = ServiceLoader.load(GameModule::class.java, loader).toList()

  /**
   * The one [GameModule] [loader] lists as a service. Fails, naming what it found, when there is
   * none or more than one.
   */
  public fun load(
    loader: ClassLoader = checkNotNull(GameModules::class.java.classLoader)
  ): GameModule {
    val found = ServiceLoader.load(GameModule::class.java, loader).toList()
    val service = "META-INF/services/${GameModule::class.java.name}"
    return when (found.size) {
      1 -> found.single()
      0 -> error("No game on the classpath: a game lists its GameModule in $service")
      else ->
        error(
          "Several games on the classpath, expected one: " +
            found.joinToString { it.javaClass.name }
        )
    }
  }
}
