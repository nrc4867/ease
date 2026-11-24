package dev.chieppa.ease.net.snapshot

/** Told, for each replicated component the host holds, which fields changed since the last tick. */
internal fun interface ChangeSink {
  /**
   * The component in [slot], on the entity with network id [networkId], changed [fields] of the
   * [allFields] it has; [fields] is 0 when nothing changed.
   */
  fun changed(slot: Int, networkId: Long, fields: Int, allFields: Int)
}
