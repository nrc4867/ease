package dev.chieppa.ease.cli.hook

import dev.chieppa.ease.cli.project.ProjectFile
import java.nio.file.Path

/** A project `ease new` has just written: where it is and what its project file says. */
public class GeneratedProject(
  /** The project's root folder. */
  public val directory: Path,
  /** The contents of its project file. */
  public val file: ProjectFile,
)
