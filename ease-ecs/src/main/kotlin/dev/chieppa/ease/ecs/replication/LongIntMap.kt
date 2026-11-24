package dev.chieppa.ease.ecs.replication

private const val EMPTY = 0L
private const val INITIAL_SLOTS = 64
private const val GOLDEN = -0x61c8864680b583ebL
private const val HASH_SHIFT = 32

/**
 * Maps positive longs to ints without boxing: open addressing with linear probing, so lookups on
 * hot paths allocate nothing. Zero marks an empty slot, so zero is never a key.
 */
internal class LongIntMap {
  private var keys = LongArray(INITIAL_SLOTS)
  private var values = IntArray(INITIAL_SLOTS)
  private var count = 0

  /** The value under [key], or [missing] when there is none. */
  fun get(key: Long, missing: Int): Int {
    if (key <= EMPTY) return missing
    var slot = slotOf(key)
    while (keys[slot] != key && keys[slot] != EMPTY) slot = (slot + 1) and (keys.size - 1)
    return if (keys[slot] == key) values[slot] else missing
  }

  /** Puts [value] under [key], which must be positive, replacing any value there. */
  fun put(key: Long, value: Int) {
    require(key > EMPTY) { "Keys are positive, not $key" }
    if ((count + 1) * 2 > keys.size) grow()
    var slot = slotOf(key)
    while (keys[slot] != EMPTY && keys[slot] != key) slot = (slot + 1) and (keys.size - 1)
    if (keys[slot] == EMPTY) count++
    keys[slot] = key
    values[slot] = value
  }

  /** Removes [key], shifting later entries of its probe run back so lookups still find them. */
  fun remove(key: Long) {
    if (key <= EMPTY) return
    var slot = slotOf(key)
    while (keys[slot] != key) {
      if (keys[slot] == EMPTY) return
      slot = (slot + 1) and (keys.size - 1)
    }
    keys[slot] = EMPTY
    count--
    shiftBackAfter(slot)
  }

  private fun shiftBackAfter(removed: Int) {
    val mask = keys.size - 1
    var hole = removed
    var slot = (removed + 1) and mask
    while (keys[slot] != EMPTY) {
      val home = slotOf(keys[slot])
      if (((slot - home) and mask) >= ((slot - hole) and mask)) {
        keys[hole] = keys[slot]
        values[hole] = values[slot]
        keys[slot] = EMPTY
        hole = slot
      }
      slot = (slot + 1) and mask
    }
  }

  private fun grow() {
    val oldKeys = keys
    val oldValues = values
    keys = LongArray(oldKeys.size * 2)
    values = IntArray(oldKeys.size * 2)
    count = 0
    oldKeys.indices.filter { oldKeys[it] != EMPTY }.forEach { put(oldKeys[it], oldValues[it]) }
  }

  private fun slotOf(key: Long): Int {
    val mixed = key * GOLDEN
    return (mixed xor (mixed ushr HASH_SHIFT)).toInt() and (keys.size - 1)
  }
}
