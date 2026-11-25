package dev.chieppa.ease.project.scene

/** What is wrong with a scene file that did not read. */
public enum class SceneFileFault {
  /** The file is missing, is not valid YAML, or does not hold what its format needs. */
  UNREADABLE,

  /** The file holds a node id that an earlier file of the folder already holds. */
  REPEATED_ID,
}
