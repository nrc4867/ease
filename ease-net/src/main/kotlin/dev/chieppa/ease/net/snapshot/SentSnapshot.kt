package dev.chieppa.ease.net.snapshot

private const val INITIAL_ENTRIES = 64

/** The components one sent snapshot carried, kept until the peer acknowledges it or it ages out. */
internal class SentSnapshot {
  /** The tick it was sent on, or [Snapshots.NO_TICK] for an unused record. */
  var tick: Long = Snapshots.NO_TICK
    private set

  /** Whether the peer's acknowledgement has been applied. */
  var acknowledged: Boolean = false

  /** How many components it carried. */
  var count: Int = 0
    private set

  private var slots = IntArray(INITIAL_ENTRIES)
  private var networkIds = LongArray(INITIAL_ENTRIES)

  /** Starts the record of the snapshot sent on [tick]. */
  fun start(tick: Long) {
    this.tick = tick
    acknowledged = false
    count = 0
  }

  /** Records that it carried [slot] of the entity [networkId]. */
  fun add(slot: Int, networkId: Long) {
    if (count == slots.size) {
      slots = slots.copyOf(count * 2)
      networkIds = networkIds.copyOf(count * 2)
    }
    slots[count] = slot
    networkIds[count] = networkId
    count++
  }

  /** Forgets the entries from [index] on, such as those of an entity taken back out. */
  fun truncate(index: Int) {
    count = index
  }

  /** The slot of entry [index]. */
  fun slotAt(index: Int): Int = slots[index]

  /** The network id of entry [index]. */
  fun networkIdAt(index: Int): Long = networkIds[index]
}
