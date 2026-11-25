package dev.chieppa.ease.project.check

/** What a [Finding] is about; [id] is the name reports print. */
public enum class FindingKind(public val id: String) {
  /** A source file with no `.meta` sidecar. */
  MISSING_SIDECAR("missing-sidecar"),

  /** A `.meta` sidecar whose source file is gone. */
  ORPHAN_SIDECAR("orphan-sidecar"),

  /** A `.meta` sidecar or source that cannot be read. */
  UNREADABLE_ASSET("unreadable-asset"),

  /** One GUID in several sidecars. */
  DUPLICATE_GUID("duplicate-guid"),

  /** A reference to an asset the project does not have. */
  DANGLING_REFERENCE("dangling-reference"),

  /** A scene or node file that cannot be read. */
  UNREADABLE_SCENE_FILE("unreadable-scene-file"),

  /** One node id in several files of a scene. */
  DUPLICATE_NODE_ID("duplicate-node-id"),

  /** A node whose parent is not in its scene. */
  MISSING_PARENT("missing-parent"),

  /** Nodes that are each other's ancestors. */
  PARENT_CYCLE("parent-cycle"),

  /** A scene or prefab that places itself, directly or through others. */
  PLACEMENT_CYCLE("placement-cycle"),

  /** An override naming a node or component the placed asset lacks. */
  STALE_OVERRIDE("stale-override"),

  /** Siblings that share an order key. */
  EQUAL_ORDER_KEYS("equal-order-keys"),

  /** Conflict markers left in a text file. */
  CONFLICT_MARKERS("conflict-markers"),

  /** A binary file git holds itself that belongs in Git LFS. */
  LFS_UNCOVERED("lfs-uncovered"),

  /** Git LFS is not installed. */
  LFS_NOT_INSTALLED("lfs-not-installed"),
}
