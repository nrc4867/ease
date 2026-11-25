package dev.chieppa.ease.physics.volume

/**
 * For each sensor, by entity id: its collision layer and the trigger entities it was inside at the
 * last step, at most [capacity] of them.
 */
internal class SensorTable(private val capacity: Int) {
  private var layers = IntArray(INITIAL_SENSORS)
  private var counts = IntArray(INITIAL_SENSORS)
  private var triggers = IntArray(INITIAL_SENSORS * capacity)

  fun add(id: Int, layer: Int) {
    grow(id)
    layers[id] = layer
    counts[id] = 0
  }

  fun remove(id: Int) {
    if (id < counts.size) counts[id] = 0
  }

  fun layerOf(id: Int): Int = layers[id]

  fun count(id: Int): Int = counts[id]

  fun at(id: Int, index: Int): Int = triggers[id * capacity + index]

  fun has(id: Int, trigger: Int): Boolean {
    for (index in 0 until counts[id]) if (at(id, index) == trigger) return true
    return false
  }

  fun set(id: Int, ids: IntArray, count: Int) {
    System.arraycopy(ids, 0, triggers, id * capacity, count)
    counts[id] = count
  }

  private fun grow(id: Int) {
    if (id < layers.size) return
    val size = maxOf(id + 1, layers.size * 2)
    layers = layers.copyOf(size)
    counts = counts.copyOf(size)
    triggers = triggers.copyOf(size * capacity)
  }

  private companion object {
    const val INITIAL_SENSORS = 64
  }
}
