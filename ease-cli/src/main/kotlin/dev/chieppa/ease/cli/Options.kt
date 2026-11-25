package dev.chieppa.ease.cli

/** The names of the `--options` commands share. */
public object Options {
  /** `--name`: what a project or a generated type is called. */
  public const val NAME: String = "name"

  /** `--package`: the package a new game's types live in. */
  public const val PACKAGE: String = "package"

  /** `--dir`: the folder a command works in. */
  public const val DIR: String = "dir"

  /** `--ease-version`: the Ease version a new game pins. */
  public const val EASE_VERSION: String = "ease-version"
}
