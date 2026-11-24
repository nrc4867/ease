package dev.chieppa.ease.net.snapshot

private const val INITIAL_SLOTS = 256

/** A tick later than every tick: a slot whose pending values no snapshot has carried yet. */
private const val NOT_CARRIED = Long.MAX_VALUE

/**
 * One peer's view of every replicated component: the fields it may not hold yet, and the first
 * snapshot that carried their current values. A field stays pending, and goes in every snapshot,
 * until the peer acknowledges a snapshot that carried its current value; so whatever a snapshot
 * leaves out, the peer already holds.
 */
internal class SlotStates {
  private var pending = IntArray(INITIAL_SLOTS)
  private var firstCarried = LongArray(INITIAL_SLOTS) { NOT_CARRIED }
  private var networkIds = LongArray(INITIAL_SLOTS)

  /** The fields of [slot] the peer may not hold yet. */
  fun pending(slot: Int): Int = if (slot < pending.size) pending[slot] else 0

  /**
   * Folds a tick's changes into [slot]: every field when the slot is new to this peer or holds a
   * different entity than before, else [fields]. New values make the slot uncarried again.
   */
  fun fold(slot: Int, networkId: Long, fields: Int, allFields: Int) {
    if (slot >= pending.size) grow(slot)
    when {
      networkIds[slot] != networkId -> {
        networkIds[slot] = networkId
        pending[slot] = allFields
        firstCarried[slot] = NOT_CARRIED
      }
      fields != 0 -> {
        pending[slot] = pending[slot] or fields
        firstCarried[slot] = NOT_CARRIED
      }
    }
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
    if (slot >= pending.size || networkIds[slot] != networkId) return
    if (firstCarried[slot] > tick) return
    pending[slot] = 0
    firstCarried[slot] = NOT_CARRIED
  }

  private fun grow(slot: Int) {
    val size = maxOf(slot + 1, pending.size * 2)
    val oldSize = pending.size
    pending = pending.copyOf(size)
    networkIds = networkIds.copyOf(size)
    firstCarried = firstCarried.copyOf(size)
    firstCarried.fill(NOT_CARRIED, oldSize, size)
  }
}
