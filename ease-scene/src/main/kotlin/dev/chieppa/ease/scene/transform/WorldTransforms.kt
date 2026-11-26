package dev.chieppa.ease.scene.transform

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.scene.node.Parent
import dev.chieppa.ease.scene.runtime.SceneNodes

private const val QUAT_W = 3
private const val NEVER = -1L
private const val IN_PROGRESS = -2L

/**
 * Each node's transform in the world: its [LocalTransform] placed by its parent's world transform,
 * up the tree of [Parent] keys, so a child follows its parent. A node with no parent, or whose
 * parent is not live, is placed by its local transform alone. Positions are scaled, rotated and
 * moved by the parent; rotations compose; scales multiply axis by axis, so a parent's non-uniform
 * scale under a rotated child is kept per axis, without shear.
 *
 * It is derived state: worked out again every tick from the local transforms, never saved or
 * replicated, so prediction, replay and replication only ever touch local transforms. It runs in
 * `Phase.EXTRACT` ahead of the renderer's extract, starting a new pass; [write] gives any reader,
 * physics included, an entity's world transform in the current pass, working it out on demand when
 * the pass has not reached it yet ([invalidate] starts a pass early). Arrays are indexed by entity
 * id and grow only when an id passes them, so a steady world allocates nothing.
 *
 * A node [posed] answers for, such as an animated bone, is placed by the transform it gives in
 * place of its `LocalTransform`, so its children follow the pose.
 */
public class WorldTransforms(initialCapacity: Int = DEFAULT_CAPACITY) :
  IteratingSystem(family { all(LocalTransform, Parent) }) {
  private var values = FloatArray(initialCapacity * WORLD_TRANSFORM_FLOATS)
  private var stamps = LongArray(initialCapacity) { NEVER }
  private var pass = 0L
  private val local = FloatArray(WORLD_TRANSFORM_FLOATS)
  private val math = TransformMath()
  private var nodes: SceneNodes? = null

  /**
   * What places posed nodes, such as an animation system's bones; null places every node by its
   * `LocalTransform`.
   */
  public var posed: PosedTransforms? = null

  override fun onInit() {
    nodes = world.systemOrNull<SceneNodes>()
  }

  override fun onTick() {
    invalidate()
    super.onTick()
  }

  /**
   * Starts a new pass: every world transform is worked out again when next read. A system that
   * reads world transforms before this one runs in a tick, after something moved, calls it first.
   */
  public fun invalidate() {
    pass++
  }

  override fun onTickEntity(entity: Entity) {
    resolve(entity)
  }

  /**
   * Writes [entity]'s world transform, [WORLD_TRANSFORM_FLOATS] floats, into [dst] at [at]. An
   * entity with no [Parent] writes its local transform.
   */
  public fun write(entity: Entity, dst: FloatArray, at: Int) {
    with(world) {
      if (entity hasNo Parent) {
        writeOwn(entity, dst, at)
        return
      }
    }
    resolve(entity)
    System.arraycopy(values, entity.id * WORLD_TRANSFORM_FLOATS, dst, at, WORLD_TRANSFORM_FLOATS)
  }

  /** Works out [entity]'s world transform for this pass, parents first, and stores it. */
  private fun resolve(entity: Entity) {
    val id = entity.id
    ensureCapacity(id + 1)
    if (stamps[id] == pass) return
    val base = id * WORLD_TRANSFORM_FLOATS
    stamps[id] = IN_PROGRESS
    // Writing a posed node can resolve other nodes and grow the store, so the write may land in the
    // array this pass started with: carry it over.
    val written = values
    val own = writeOwn(entity, written, base)
    if (written !== values) System.arraycopy(written, base, values, base, WORLD_TRANSFORM_FLOATS)
    val parent = if (own == PosedTransforms.WORLD) Entity.NONE else parentOf(entity)
    if (parent != Entity.NONE && stamps.getOrElse(parent.id) { NEVER } != IN_PROGRESS) {
      val parentBase = parentWorld(parent)
      compose(parentBase, base)
    }
    stamps[id] = pass
  }

  /** Where [parent]'s world transform is in [values], resolving it first when it has a parent. */
  private fun parentWorld(parent: Entity): Int {
    val hasParent = with(world) { parent has Parent }
    if (hasParent) {
      resolve(parent)
      return parent.id * WORLD_TRANSFORM_FLOATS
    }
    writeOwn(parent, local, 0)
    return -1
  }

  /**
   * Writes [entity]'s own transform into [dst] at [at]: its pose when [posed] gives one, else its
   * local transform. Returns what [PosedTransforms.write] said, [PosedTransforms.UNPOSED] for a
   * local transform.
   */
  private fun writeOwn(entity: Entity, dst: FloatArray, at: Int): Int {
    val result = posed?.write(entity, dst, at) ?: PosedTransforms.UNPOSED
    if (result == PosedTransforms.UNPOSED) writeLocal(world, entity, dst, at)
    return result
  }

  private fun parentOf(entity: Entity): Entity {
    val index = nodes?.index ?: return Entity.NONE
    val parent = with(world) { entity[Parent] }
    val found = index[parent.node]
    return if (found != Entity.NONE && with(world) { found has LocalTransform }) found
    else Entity.NONE
  }

  /**
   * Places the local transform at [base] in [values] by the parent's, at [parentBase] or [local].
   */
  private fun compose(parentBase: Int, base: Int) {
    val parent = if (parentBase < 0) local else values
    val p = if (parentBase < 0) 0 else parentBase
    math.compose(parent, p, values, base, values, base)
  }

  private fun ensureCapacity(needed: Int) {
    if (needed <= stamps.size) return
    val grown = maxOf(needed, stamps.size * 2)
    values = values.copyOf(grown * WORLD_TRANSFORM_FLOATS)
    val old = stamps.size
    stamps = stamps.copyOf(grown).also { it.fill(NEVER, old) }
  }

  public companion object {
    /** Entities the arrays hold before they first grow. */
    public const val DEFAULT_CAPACITY: Int = 1024

    /** Writes [entity]'s local transform of [world] into [dst] at [at]. */
    public fun writeLocal(world: World, entity: Entity, dst: FloatArray, at: Int) {
      val t = with(world) { entity[LocalTransform] }
      dst[at] = t.x
      dst[at + 1] = t.y
      dst[at + 2] = t.z
      dst[at + TRANSFORM_ROTATION_AT] = t.rotationX
      dst[at + TRANSFORM_ROTATION_AT + 1] = t.rotationY
      dst[at + TRANSFORM_ROTATION_AT + 2] = t.rotationZ
      dst[at + TRANSFORM_ROTATION_AT + QUAT_W] = t.rotationW
      dst[at + TRANSFORM_SCALE_AT] = t.scaleX
      dst[at + TRANSFORM_SCALE_AT + 1] = t.scaleY
      dst[at + TRANSFORM_SCALE_AT + 2] = t.scaleZ
    }
  }
}
