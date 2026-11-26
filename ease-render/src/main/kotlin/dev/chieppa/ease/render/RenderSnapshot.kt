package dev.chieppa.ease.render

import dev.chieppa.ease.ecs.DEFAULT_ENTITY_CAPACITY
import dev.chieppa.ease.gpu.lit.MaterialFloats
import dev.chieppa.ease.gpu.lit.MaterialTextures
import dev.chieppa.ease.gpu.mesh.MeshData
import dev.chieppa.ease.math.Quats
import dev.chieppa.ease.render.assets.MaterialAsset
import dev.chieppa.ease.render.scene.MeshShape

/** Marks an id no extract has written yet. */
private const val NEVER = -1L

/** Marks a camera or light the extract did not find. */
internal const val NONE = -1

/** Floats of one transform: position x, y, z, rotation x, y, z, w, scale x, y, z. */
public const val TRANSFORM_FLOATS: Int = 10

/** Where the rotation starts within a transform's floats. */
public const val ROTATION_AT: Int = 3

/** Where the scale starts within a transform's floats. */
public const val SCALE_AT: Int = 7

/** Where a quaternion's w sits among its x, y, z and w. */
public const val QUAT_W: Int = 3

/**
 * The read-only view of a world that rendering works from, written once per tick by
 * [ExtractSystem]. For every entity with a transform and a mesh, camera or light it holds the world
 * transform at the previous and the current tick, so a frame between ticks can blend them; for each
 * mesh drawn this tick its shape, runtime mesh or mesh asset, its material (and its material asset,
 * when it has one), its textures and, for a skinned mesh, its skin and joint matrices in [skins];
 * and the [camera] and [light] the world is seen through and lit by. Arrays are indexed by entity
 * id and grow only when an id passes their capacity; a steady world writes it without allocating.
 */
public class RenderSnapshot(initialCapacity: Int = DEFAULT_ENTITY_CAPACITY) {
  init {
    require(initialCapacity > 0) { "Needs room for at least 1 entity, got $initialCapacity" }
  }

  internal var previous = FloatArray(initialCapacity * TRANSFORM_FLOATS)
  internal var current = FloatArray(initialCapacity * TRANSFORM_FLOATS)
  internal var materials = FloatArray(initialCapacity * MaterialFloats.COUNT)
  internal var shapes = arrayOfNulls<MeshShape>(initialCapacity)
  internal var customMeshes = arrayOfNulls<MeshData>(initialCapacity)
  internal var textures = arrayOfNulls<MaterialTextures>(initialCapacity)
  internal var materialAssets = arrayOfNulls<MaterialAsset>(initialCapacity)
  private var writtenAt = LongArray(initialCapacity) { NEVER }
  private var drawn = IntArray(initialCapacity)

  /** This tick's skinned meshes and their joint matrices. */
  public val skins: SnapshotSkins = SnapshotSkins(initialCapacity)

  /** The camera found this tick. */
  public val camera: SnapshotCamera = SnapshotCamera()

  /** The light found this tick. */
  public val light: SnapshotLight = SnapshotLight()

  /** How many extracts have run; the one under way while [ExtractSystem] writes. */
  public var extracts: Long = 0
    private set

  /** How many meshes the last extract found. */
  public var count: Int = 0
    private set

  /** The entity id of the [index]th mesh, from 0 until [count]. */
  public fun entityAt(index: Int): Int {
    require(index in 0 until count) { "No mesh $index among $count" }
    return drawn[index]
  }

  /** The built-in shape entity [id] draws, or null when it draws a runtime mesh. */
  public fun shape(id: Int): MeshShape? = shapes[id]

  /** The runtime or asset mesh entity [id] draws, or null when it draws a built-in shape. */
  public fun customMesh(id: Int): MeshData? = customMeshes[id]

  /** The textures entity [id] draws with, or null for the defaults. */
  public fun textures(id: Int): MaterialTextures? = textures[id]

  /** Material float [field] of entity [id], packed as [MaterialFloats] says. */
  public fun material(id: Int, field: Int): Float = materials[id * MaterialFloats.COUNT + field]

  /**
   * Writes entity [id]'s transform blended [alpha] of the way from the previous tick to this one
   * into [dst] at [at], [TRANSFORM_FLOATS] floats: positions and scales blended linearly, the
   * rotation along the shortest arc.
   */
  public fun transformInto(id: Int, alpha: Float, dst: FloatArray, at: Int) {
    val base = id * TRANSFORM_FLOATS
    for (axis in 0 until ROTATION_AT) dst[at + axis] = lerp(base + axis, alpha)
    for (axis in SCALE_AT until TRANSFORM_FLOATS) dst[at + axis] = lerp(base + axis, alpha)
    Quats.slerp(
      previous,
      base + ROTATION_AT,
      current,
      base + ROTATION_AT,
      alpha,
      dst,
      at + ROTATION_AT,
    )
  }

  /** Starts a new extract: what is written after it is what is drawn this tick. */
  internal fun begin() {
    extracts++
    count = 0
    skins.begin()
    camera.entity = NONE
    light.entity = NONE
  }

  /**
   * Writes entity [id]'s transform, [transform] holding [TRANSFORM_FLOATS] floats, moving the last
   * one it wrote to previous; an entity not written at the last extract starts where it is.
   */
  internal fun writeTransform(id: Int, transform: FloatArray) {
    ensureCapacity(maxOf(id, count) + 1)
    val base = id * TRANSFORM_FLOATS
    if (writtenAt[id] == extracts - 1) {
      System.arraycopy(current, base, previous, base, TRANSFORM_FLOATS)
    } else {
      System.arraycopy(transform, 0, previous, base, TRANSFORM_FLOATS)
    }
    System.arraycopy(transform, 0, current, base, TRANSFORM_FLOATS)
    writtenAt[id] = extracts
  }

  /** Lists entity [id] among this tick's meshes, drawing [shape] or [custom] with [surface]. */
  internal fun addMesh(id: Int, shape: MeshShape?, custom: MeshData?, surface: MaterialTextures?) {
    skins.clear(id)
    shapes[id] = shape
    customMeshes[id] = custom
    textures[id] = surface
    drawn[count++] = id
  }

  private fun ensureCapacity(needed: Int) {
    if (needed <= writtenAt.size) return
    val grown = maxOf(needed, writtenAt.size * 2)
    previous = previous.copyOf(grown * TRANSFORM_FLOATS)
    current = current.copyOf(grown * TRANSFORM_FLOATS)
    materials = materials.copyOf(grown * MaterialFloats.COUNT)
    shapes = shapes.copyOf(grown)
    customMeshes = customMeshes.copyOf(grown)
    textures = textures.copyOf(grown)
    materialAssets = materialAssets.copyOf(grown)
    val old = writtenAt.size
    writtenAt = writtenAt.copyOf(grown).also { it.fill(NEVER, old) }
    drawn = drawn.copyOf(grown)
    skins.grow(grown)
  }

  private fun lerp(at: Int, alpha: Float): Float =
    previous[at] + (current[at] - previous[at]) * alpha
}
