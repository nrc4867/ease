package dev.chieppa.ease.commands.wire

/** The limits of the compact wire form that a decoder holds received bits to. */
internal object Wire {
  /** The deepest values may nest: classes, collections and polymorphic values each count one. */
  const val MAX_DEPTH: Int = 32

  /** The most elements one collection may hold. */
  const val MAX_COLLECTION: Long = 4096
}
