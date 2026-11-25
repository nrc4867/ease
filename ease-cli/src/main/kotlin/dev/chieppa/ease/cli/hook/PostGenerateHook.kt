package dev.chieppa.ease.cli.hook

/**
 * A step that runs once `ease new` has written a project, in the order the hooks are listed, such
 * as preparing version control. A hook that throws stops the command with its message.
 */
public fun interface PostGenerateHook {
  /** Runs the step on [project]. */
  public fun run(project: GeneratedProject)
}
