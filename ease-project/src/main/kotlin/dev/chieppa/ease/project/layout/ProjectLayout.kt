package dev.chieppa.ease.project.layout

import java.nio.file.Path

/** Where the parts of a project are, relative to its root folder. */
public object ProjectLayout {
  /** The folder holding the project's assets; every asset path is relative to it. */
  public const val CONTENT_FOLDER: String = "content"

  /** The folder git keeps its repository in. */
  public const val GIT_FOLDER: String = ".git"

  /** Ease's own folder inside a project: caches and per-user state, all ignored by git. */
  public const val EASE_FOLDER: String = ".ease"

  /** The folder of [project] holding its assets. */
  public fun content(project: Path): Path = project.resolve(CONTENT_FOLDER)

  /** Whether [project] is the root of a git repository. */
  public fun isRepository(project: Path): Boolean = project.resolve(GIT_FOLDER).toFile().exists()

  /** [path] under [project] as written in reports and git: relative, with `/` between names. */
  public fun display(project: Path, path: Path): String = project.relativize(path).joinToString("/")
}
