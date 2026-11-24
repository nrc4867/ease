package dev.chieppa.ease.render

import dev.chieppa.ease.ecs.DEFAULT_ENTITY_CAPACITY

/** Marks an id no extract has written yet. */
private const val NEVER = -1L

/**
 * The read-only view of a world that rendering works from, written once per tick by
 * [ExtractSystem]. For each quad drawn this tick it holds the position at the previous and the
 * current tick, so a frame between ticks can blend them, plus the quad's size and color. Columns
 * are indexed by entity id and grow only when an id passes their capacity; a steady world writes it
 * without allocating.
 */
public class RenderSnapshot(initialCapacity: Int = DEFAULT_ENTITY_CAPACITY) {
  init {
    require(initialCapacity > 0) { "Needs room for at least 1 entity, got $initialCapacity" }
  }

  /** Each entity's x at the previous tick; its current x when it first appeared this tick. */
  public val previousX: SnapshotColumn = SnapshotColumn(initialCapacity)

  /** Each entity's y at the previous tick; its current y when it first appeared this tick. */
  public val previousY: SnapshotColumn = SnapshotColumn(initialCapacity)

  /** Each entity's x at this tick. */
  public val currentX: SnapshotColumn = SnapshotColumn(initialCapacity)

  /** Each entity's y at this tick. */
  public val currentY: SnapshotColumn = SnapshotColumn(initialCapacity)

  /** The side length of each entity's quad. */
  public val size: SnapshotColumn = SnapshotColumn(initialCapacity)

  /** The red channel of each entity's quad. */
  public val red: SnapshotColumn = SnapshotColumn(initialCapacity)

  /** The green channel of each entity's quad. */
  public val green: SnapshotColumn = SnapshotColumn(initialCapacity)

  /** The blue channel of each entity's quad. */
  public val blue: SnapshotColumn = SnapshotColumn(initialCapacity)

  private val columns = listOf(previousX, previousY, currentX, currentY, size, red, green, blue)
  private var writtenAt = LongArray(initialCapacity) { NEVER }
  private var drawn = IntArray(initialCapacity)

  /** How many extracts have run; the one under way while [ExtractSystem] writes. */
  public var extracts: Long = 0
    private set

  /** How many quads the last extract found. */
  public var count: Int = 0
    private set

  /** The entity id of the [index]th quad, from 0 until [count]. */
  public fun entityAt(index: Int): Int {
    require(index in 0 until count) { "No quad $index among $count" }
    return drawn[index]
  }

  /** Entity [id]'s x blended [alpha] of the way from the previous tick to this one. */
  public fun x(id: Int, alpha: Float): Float = lerp(previousX[id], currentX[id], alpha)

  /** Entity [id]'s y blended [alpha] of the way from the previous tick to this one. */
  public fun y(id: Int, alpha: Float): Float = lerp(previousY[id], currentY[id], alpha)

  /** Starts a new extract: the quads written after it are the ones drawn this tick. */
  internal fun begin() {
    extracts++
    count = 0
  }

  /** Writes entity [id] at ([x], [y]) with [quad]'s look, moving its last position to previous. */
  internal fun write(id: Int, x: Float, y: Float, quad: Quad) {
    ensureCapacity(maxOf(id, count) + 1)
    val seenLastTick = writtenAt[id] == extracts - 1
    previousX[id] = if (seenLastTick) currentX[id] else x
    previousY[id] = if (seenLastTick) currentY[id] else y
    currentX[id] = x
    currentY[id] = y
    size[id] = quad.size
    red[id] = quad.red
    green[id] = quad.green
    blue[id] = quad.blue
    writtenAt[id] = extracts
    drawn[count++] = id
  }

  private fun ensureCapacity(needed: Int) {
    if (needed <= writtenAt.size) return
    val grown = maxOf(needed, writtenAt.size * 2)
    columns.forEach { it.grow(grown) }
    val old = writtenAt.size
    writtenAt = writtenAt.copyOf(grown).also { it.fill(NEVER, old) }
    drawn = drawn.copyOf(grown)
  }

  private fun lerp(from: Float, to: Float, alpha: Float): Float = from + (to - from) * alpha
}
