package dev.chieppa.ease.assets

/** Where new asset GUIDs come from, so tests can hand out known ones. */
public fun interface GuidSource {
  /** A GUID no asset has. */
  public fun next(): Guid

  /** The default: random version 4 GUIDs. */
  public companion object {
    /** Random version 4 GUIDs. */
    public val random: GuidSource = GuidSource { Guid.random() }
  }
}
