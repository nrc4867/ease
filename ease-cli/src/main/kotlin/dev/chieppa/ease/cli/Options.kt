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

  /** `--no-git`: leave a new project without a git repository. */
  public const val NO_GIT: String = "no-git"

  /** `--fix`: let `ease check` make the mechanical fixes. */
  public const val FIX: String = "fix"

  /** `--staged`: let `ease check` look at what git's index holds. */
  public const val STAGED: String = "staged"

  /** `--adopt`: let `ease import` give new sources a meta file first. */
  public const val ADOPT: String = "adopt"

  /** The options that take no value; naming one switches it on. */
  public val FLAGS: Set<String> = setOf(NO_GIT, FIX, STAGED, ADOPT)
}
