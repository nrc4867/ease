package dev.chieppa.ease.render

/**
 * One value per entity in a [RenderSnapshot], read by entity id. Read-only to rendering; the
 * snapshot swaps in a larger array when it grows.
 */
public class SnapshotColumn internal constructor(capacity: Int) {
  internal var values: FloatArray = FloatArray(capacity)

  /** The value for entity [id]. */
  public operator fun get(id: Int): Float = values[id]

  internal operator fun set(id: Int, value: Float) {
    values[id] = value
  }

  internal fun grow(capacity: Int) {
    values = values.copyOf(capacity)
  }
}
