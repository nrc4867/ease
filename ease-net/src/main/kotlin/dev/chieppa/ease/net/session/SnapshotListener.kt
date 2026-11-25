package dev.chieppa.ease.net.session

/**
 * Told about each snapshot a [ClientSession] applies, as it applies it, so a client can keep what
 * that snapshot brought before the next one changes it.
 */
public fun interface SnapshotListener {
  /** The snapshot of the host's [tick] was just applied to the client's world. */
  public fun applied(tick: Long)

  public companion object {
    /** Listens to nothing. */
    public val NONE: SnapshotListener = SnapshotListener {}
  }
}
