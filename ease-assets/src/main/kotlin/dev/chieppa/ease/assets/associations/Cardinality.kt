package dev.chieppa.ease.assets.associations

/** How many contributors of one kind a single file may have. */
public enum class Cardinality {
  /** Exactly one wins; two equally specific ones with the same priority are an error. */
  ONE,

  /** Every match applies, best first. */
  MANY,
}
