package dev.chieppa.ease.net.snapshot

private const val INITIAL_SLOTS = 256

/** A tick later than every tick: a slot whose pending values no snapshot has carried yet. */
private const val NOT_CARRIED = Long.MAX_VALUE

/** The network id of no entity: a slot the peer does not know. */
private const val UNKNOWN = 0L

/**
 * One peer's view of every replicated component: which components it knows (the entity, under its
 * network id, and the component on it), the fields it may not hold yet, and the first snapshot that
 * carried their current values. A field stays pending, and goes in every snapshot, until the peer
 * acknowledges a snapshot that carried its current value; so whatever a snapshot leaves out, the
 * peer already holds. A component the peer does not know is never sent, since it could not apply.
 */
internal class SlotStates {
  private var pending = IntArray(INITIAL_SLOTS)
  private var firstCarried = LongArray(INITIAL_SLOTS) { NOT_CARRIED }
  private var known = LongArray(INITIAL_SLOTS)

  /** Records that the peer holds [slot] on the entity [networkId]; every field starts pending. */
  fun know(slot: Int, networkId: Long, allFields: Int) {
    if (slot >= pending.size) grow(slot)
    known[slot] = networkId
    pending[slot] = allFields
    firstCarried[slot] = NOT_CARRIED
  }

  /** Whether the peer holds [slot] on the entity [networkId]. */
  fun knows(slot: Int, networkId: Long): Boolean =
    slot < known.size && networkId != UNKNOWN && known[slot] == networkId

  /** The fields of [slot] on the entity [networkId] the peer may not hold yet. */
  fun pending(slot: Int, networkId: Long): Int = if (knows(slot, networkId)) pending[slot] else 0

  /** Folds a tick's changed [fields] into [slot]; new values make it uncarried again. */
  fun fold(slot: Int, networkId: Long, fields: Int) {
    if (fields == 0 || !knows(slot, networkId)) return
    pending[slot] = pending[slot] or fields
    firstCarried[slot] = NOT_CARRIED
  }

  /** Records that the snapshot of [tick] carried [slot]'s pending fields. */
  fun carried(slot: Int, tick: Long) {
    if (firstCarried[slot] == NOT_CARRIED) firstCarried[slot] = tick
  }

  /**
   * The peer applied the snapshot of [tick], which carried [slot] for the entity [networkId]: when
   * that snapshot held the slot's current values, nothing in it is pending any more.
   */
  fun confirm(slot: Int, networkId: Long, tick: Long) {
    if (!knows(slot, networkId) || firstCarried[slot] > tick) return
    pending[slot] = 0
    firstCarried[slot] = NOT_CARRIED
  }

  private fun grow(slot: Int) {
    val size = maxOf(slot + 1, pending.size * 2)
    val oldSize = pending.size
    pending = pending.copyOf(size)
    known = known.copyOf(size)
    firstCarried = firstCarried.copyOf(size)
    firstCarried.fill(NOT_CARRIED, oldSize, size)
  }
}
