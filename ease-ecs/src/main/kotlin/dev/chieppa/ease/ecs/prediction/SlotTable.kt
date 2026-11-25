package dev.chieppa.ease.ecs.prediction

private const val INITIAL_SLOTS = 64

/**
 * Per-entity state of type [S], by entity id, and which entities are active this tick, in the order
 * they were activated. It grows when an id passes its capacity and otherwise allocates nothing.
 */
internal class SlotTable<S : Any> {
  private var slots = arrayOfNulls<Any>(INITIAL_SLOTS)
  private var active = IntArray(INITIAL_SLOTS)

  /** How many entities are active. */
  var activeCount: Int = 0
    private set

  /** The state kept for entity [id], or null. */
  @Suppress("UNCHECKED_CAST") // Only states of type S are stored.
  operator fun get(id: Int): S? = if (id < slots.size) slots[id] as S? else null

  /** Keeps [slot] for entity [id]. */
  operator fun set(id: Int, slot: S) {
    if (id >= slots.size) slots = slots.copyOf(maxOf(id + 1, slots.size * 2))
    slots[id] = slot
  }

  /** Makes no entity active. */
  fun deactivateAll() {
    activeCount = 0
  }

  /** Makes entity [id] active. */
  fun activate(id: Int) {
    if (activeCount == active.size) active = active.copyOf(active.size * 2)
    active[activeCount++] = id
  }

  /** The state of the [index]th active entity. */
  fun activeAt(index: Int): S = checkNotNull(get(active[index]))
}
