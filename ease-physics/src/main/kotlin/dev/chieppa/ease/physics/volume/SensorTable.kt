package dev.chieppa.ease.physics.volume

/**
 * For each sensor, by entity id, the trigger entities it was inside at the last step, at most
 * [capacity] of them.
 */
internal class SensorTable(private val capacity: Int) {
  private var counts = IntArray(INITIAL_SENSORS)
  private var triggers = IntArray(INITIAL_SENSORS * capacity)

  /** How many sensor ids the table has room for; every id below it may be asked about. */
  val size: Int
    get() = counts.size

  fun count(id: Int): Int = if (id < counts.size) counts[id] else 0

  fun at(id: Int, index: Int): Int = triggers[id * capacity + index]

  fun has(id: Int, trigger: Int): Boolean {
    var found = false
    for (index in 0 until count(id)) found = found || at(id, index) == trigger
    return found
  }

  fun set(id: Int, ids: IntArray, count: Int) {
    grow(id)
    System.arraycopy(ids, 0, triggers, id * capacity, count)
    counts[id] = count
  }

  /** Forgets every trigger sensor [id] was inside. */
  fun clear(id: Int) {
    if (id < counts.size) counts[id] = 0
  }

  /** Forgets [trigger] from sensor [id]'s triggers. */
  fun drop(id: Int, trigger: Int) {
    val base = id * capacity
    var kept = 0
    for (index in 0 until count(id)) {
      val held = triggers[base + index]
      if (held != trigger) triggers[base + kept++] = held
    }
    if (id < counts.size) counts[id] = kept
  }

  private fun grow(id: Int) {
    if (id < counts.size) return
    val size = maxOf(id + 1, counts.size * 2)
    counts = counts.copyOf(size)
    triggers = triggers.copyOf(size * capacity)
  }

  private companion object {
    const val INITIAL_SENSORS = 64
  }
}
