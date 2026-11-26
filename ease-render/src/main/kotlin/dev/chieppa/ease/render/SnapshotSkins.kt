package dev.chieppa.ease.render

import dev.chieppa.ease.gpu.mesh.SkinVertices
import dev.chieppa.ease.memory.MAT4_FLOATS

/**
 * The skinned meshes of one extract: for each entity id drawn skinned, its skin and where its joint
 * matrices sit in [palette]. Arrays are indexed by entity id and the palette grows only when a tick
 * needs more joints than any before, so a steady world writes it without allocating.
 */
public class SnapshotSkins internal constructor(initialCapacity: Int) {
  private var skins = arrayOfNulls<SkinVertices>(initialCapacity)
  private var jointFrom = IntArray(initialCapacity)
  private var jointCounts = IntArray(initialCapacity)

  /** This tick's joint matrices of every skinned mesh, 16 floats a joint. */
  public var palette: FloatArray = FloatArray(INITIAL_JOINTS * MAT4_FLOATS)
    private set

  /** How many joint matrices of [palette] this tick's extract wrote. */
  public var joints: Int = 0
    private set

  /** The skin entity [id] draws its mesh with, or null when it draws it unskinned. */
  public fun skin(id: Int): SkinVertices? = skins[id]

  /** Where entity [id]'s joint matrices start in [palette], in floats. */
  public fun paletteFrom(id: Int): Int = jointFrom[id] * MAT4_FLOATS

  /** How many joint matrices entity [id]'s skin has in [palette]. */
  public fun jointCount(id: Int): Int = jointCounts[id]

  /** Starts a new extract with no joints written. */
  internal fun begin() {
    joints = 0
  }

  /** Marks entity [id] as drawn unskinned until [add] says otherwise. */
  internal fun clear(id: Int) {
    skins[id] = null
  }

  /** Room for [count] more joint matrices in [palette], from [joints] on. */
  internal fun reserve(count: Int) {
    val needed = (joints + count) * MAT4_FLOATS
    if (needed > palette.size) palette = palette.copyOf(maxOf(needed, palette.size * 2))
  }

  /** Draws entity [id]'s mesh bent by [skin], its [count] matrices just written from [joints]. */
  internal fun add(id: Int, skin: SkinVertices, count: Int) {
    skins[id] = skin
    jointFrom[id] = joints
    jointCounts[id] = count
    joints += count
  }

  /** Room for entity ids below [capacity]. */
  internal fun grow(capacity: Int) {
    skins = skins.copyOf(capacity)
    jointFrom = jointFrom.copyOf(capacity)
    jointCounts = jointCounts.copyOf(capacity)
  }

  private companion object {
    const val INITIAL_JOINTS = 256
  }
}
