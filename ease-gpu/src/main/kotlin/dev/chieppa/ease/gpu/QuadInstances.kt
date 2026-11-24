package dev.chieppa.ease.gpu

/** Floats one quad takes: center x and y, size, padding, then red, green, blue and padding. */
internal const val FLOATS_PER_QUAD = 8

private const val X = 0
private const val Y = 1
private const val SIZE = 2
private const val RED = 4
private const val GREEN = 5
private const val BLUE = 6

/**
 * Up to [capacity] axis-aligned square quads, packed the way [InstancedQuadPass] uploads them. A
 * renderer fills it again every frame: [clear], then [add] each quad and [setColor] it.
 */
public class QuadInstances(public val capacity: Int) {
  init {
    require(capacity > 0) { "Needs room for at least 1 quad, got $capacity" }
  }

  internal val data = FloatArray(capacity * FLOATS_PER_QUAD)

  /** How many quads are held. */
  public var count: Int = 0
    private set

  /** Empties the batch. */
  public fun clear() {
    count = 0
  }

  /**
   * Adds a white quad centered on ([x], [y]) with sides [size] long, in world units, and returns
   * its index. Fails when the batch is full.
   */
  public fun add(x: Float, y: Float, size: Float): Int {
    check(count < capacity) { "More than $capacity quads; make the batch larger" }
    val index = count++
    val at = index * FLOATS_PER_QUAD
    data[at + X] = x
    data[at + Y] = y
    data[at + SIZE] = size
    data[at + RED] = 1f
    data[at + GREEN] = 1f
    data[at + BLUE] = 1f
    return index
  }

  /** Colors the quad at [index], each channel from 0 to 1. */
  public fun setColor(index: Int, red: Float, green: Float, blue: Float) {
    require(index in 0 until count) { "No quad $index among $count" }
    val at = index * FLOATS_PER_QUAD
    data[at + RED] = red
    data[at + GREEN] = green
    data[at + BLUE] = blue
  }
}
