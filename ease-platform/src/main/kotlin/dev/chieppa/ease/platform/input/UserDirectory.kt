package dev.chieppa.ease.platform.input

import java.nio.file.Path

/** The per-user area where a game keeps what is the player's, such as their bindings. */
public object UserDirectory {
  /**
   * The folder for the game [game] in the user's configuration area: `%APPDATA%` on Windows,
   * `~/Library/Application Support` on macOS, `$XDG_CONFIG_HOME` or `~/.config` elsewhere.
   * [osName], [environment] and [home] default to this machine's and exist for tests.
   */
  public fun of(
    game: String,
    osName: String = System.getProperty("os.name"),
    environment: (String) -> String? = System::getenv,
    home: Path = Path.of(System.getProperty("user.home")),
  ): Path {
    val os = osName.lowercase()
    val root =
      when {
        os.startsWith("windows") ->
          absolute(environment("APPDATA")) ?: home.resolve("AppData/Roaming")
        os.startsWith("mac") -> home.resolve("Library/Application Support")
        else -> absolute(environment("XDG_CONFIG_HOME")) ?: home.resolve(".config")
      }
    return root.resolve(game)
  }

  /** The bindings file of [player] for [game]: `bindings-<player>.yaml` in [of]. */
  public fun bindingsFile(
    game: String,
    player: String = "default",
    directory: Path = of(game),
  ): Path = directory.resolve("bindings-$player.yaml")

  /** [value] as a path when it is set, not blank and absolute; otherwise null. */
  private fun absolute(value: String?): Path? =
    value?.takeIf { it.isNotBlank() }?.let(Path::of)?.takeIf { it.isAbsolute }
}
