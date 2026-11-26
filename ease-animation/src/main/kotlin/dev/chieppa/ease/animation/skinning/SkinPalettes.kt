package dev.chieppa.ease.animation.skinning

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.animation.assets.SkinAsset
import dev.chieppa.ease.animation.runtime.AnimationAssets
import dev.chieppa.ease.animation.scene.SkinRef
import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.math.Mat4Inverse
import dev.chieppa.ease.math.Mat4s
import dev.chieppa.ease.memory.MAT4_FLOATS
import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.runtime.NodeIndex
import dev.chieppa.ease.scene.runtime.SceneNodes
import dev.chieppa.ease.scene.transform.TRANSFORM_ROTATION_AT
import dev.chieppa.ease.scene.transform.TRANSFORM_SCALE_AT
import dev.chieppa.ease.scene.transform.WORLD_TRANSFORM_FLOATS
import dev.chieppa.ease.scene.transform.WorldTransforms

/**
 * Works out skinned meshes' joint matrices: for a node with a [SkinRef], each joint's world
 * transform times its inverse bind matrix, taken into the node's own space, so a drawer places the
 * skinned mesh with the node's (interpolated) model matrix and the matrices bend it into the pose.
 * Joints are found as the skin names them, by id in the same prefab placement as the node, and kept
 * per entity until a joint's node is replaced, a node comes or goes, or the assets change. Writing
 * a palette allocates nothing once its entity is bound.
 */
public class SkinPalettes(private val assets: AnimationAssets?) {
  private var bindings = arrayOfNulls<SkinBinding>(INITIAL)
  private var seenChanges = assets?.changes ?: 0
  private val transform = FloatArray(WORLD_TRANSFORM_FLOATS)
  private val inverse = FloatArray(MAT4_FLOATS)
  private val joint = FloatArray(MAT4_FLOATS)
  private val scratch = FloatArray(MAT4_FLOATS)
  private var nodesWorld: World? = null
  private var nodes: SceneNodes? = null

  /**
   * Writes [entity]'s joint matrices (16 floats each, column-major) into [dst] from [at] and
   * returns how many there are, or 0 when it is not skinned: no [SkinRef], a skin that does not
   * load, or room for fewer than all of them. World transforms come from [worlds], or local ones
   * without.
   */
  public fun write(
    world: World,
    entity: Entity,
    worlds: WorldTransforms?,
    dst: FloatArray,
    at: Int,
  ): Int {
    val binding = bindingOf(world, entity)
    val count = binding?.joints?.size ?: 0
    val written =
      binding != null &&
        at + count * MAT4_FLOATS <= dst.size &&
        meshInverse(world, entity, worlds) &&
        writeJoints(world, binding, worlds, dst, at)
    return if (written) count else 0
  }

  /** Writes the inverse of [entity]'s world matrix into [inverse]; false when it has none. */
  private fun meshInverse(world: World, entity: Entity, worlds: WorldTransforms?): Boolean {
    place(world, entity, worlds)
    Mat4s.fromTransform(
      transform,
      0,
      transform,
      TRANSFORM_ROTATION_AT,
      transform,
      TRANSFORM_SCALE_AT,
      scratch,
      0,
    )
    return Mat4Inverse.inverseAffine(scratch, 0, inverse, 0)
  }

  /** Writes [binding]'s joint matrices, in the space [inverse] takes them to, into [dst]. */
  private fun writeJoints(
    world: World,
    binding: SkinBinding,
    worlds: WorldTransforms?,
    dst: FloatArray,
    at: Int,
  ): Boolean {
    val joints = binding.joints
    val inverseBinds = binding.skin.inverseBindMatrices
    for (index in joints.indices) {
      val node = joints[index]
      val out = at + index * MAT4_FLOATS
      if (node == Entity.NONE) {
        Mat4s.identity(dst, out)
      } else {
        place(world, node, worlds)
        Mat4s.fromTransform(
          transform,
          0,
          transform,
          TRANSFORM_ROTATION_AT,
          transform,
          TRANSFORM_SCALE_AT,
          joint,
          0,
        )
        Mat4s.mul(joint, 0, inverseBinds, index * MAT4_FLOATS, scratch, 0)
        Mat4s.mul(inverse, 0, scratch, 0, dst, out)
      }
    }
    return true
  }

  /** How many joint matrices [entity] needs, 0 when it is not skinned. */
  public fun jointCount(world: World, entity: Entity): Int =
    bindingOf(world, entity)?.joints?.size ?: 0

  private fun place(world: World, entity: Entity, worlds: WorldTransforms?) {
    if (worlds != null) worlds.write(entity, transform, 0)
    else WorldTransforms.writeLocal(world, entity, transform, 0)
  }

  private fun bindingOf(world: World, entity: Entity): SkinBinding? {
    val changes = assets?.changes ?: 0
    if (changes != seenChanges) {
      seenChanges = changes
      bindings.fill(null)
    }
    val ref = with(world) { entity.getOrNull(SkinRef) }?.skin
    val index = nodesOf(world)?.index
    val key = with(world) { entity.getOrNull(NodeKey) }
    if (ref == null || index == null || key == null) return null
    if (entity.id >= bindings.size) {
      bindings = bindings.copyOf(maxOf(entity.id + 1, bindings.size * 2))
    }
    val existing = bindings[entity.id]
    return if (existing != null && existing.matches(entity, ref.guid, index)) existing
    else bind(entity, key, ref.guid, index)
  }

  /** [world]'s scene nodes, looked up once per world: finding a system searches them all. */
  private fun nodesOf(world: World): SceneNodes? {
    if (world !== nodesWorld) {
      nodesWorld = world
      nodes = world.systemOrNull<SceneNodes>()
    }
    return nodes
  }

  private fun bind(entity: Entity, key: NodeKey, guid: Guid, index: NodeIndex): SkinBinding? {
    val skin = assets?.skin(guid)
    val binding = skin?.let { SkinBinding.bind(entity, key, guid, it, index) }
    bindings[entity.id] = binding
    return binding
  }

  private companion object {
    const val INITIAL = 64
  }
}

/**
 * One skinned entity's joints, found through the scene's node index; it [matches] while every
 * joint's key still finds the same live node and no node came or went.
 */
private class SkinBinding(
  val entity: Entity,
  val guid: Guid,
  val skin: SkinAsset,
  val keys: Array<NodeKey>,
  val joints: Array<Entity>,
  val nodes: Int,
) {
  fun matches(
    entity: Entity,
    guid: Guid,
    index: NodeIndex,
  ): Boolean {
    var same = this.entity == entity && this.guid == guid && index.size == nodes
    for (joint in joints.indices) same = same && index[keys[joint]] == joints[joint]
    return same
  }

  companion object {
    fun bind(
      entity: Entity,
      key: NodeKey,
      guid: Guid,
      skin: SkinAsset,
      index: NodeIndex,
    ): SkinBinding {
      val keys =
        Array(skin.joints.size) {
          val id = LocalId.parse(skin.joints[it])
          if (key.depth > 1) key.prefix(key.depth - 1).child(id) else NodeKey.of(id)
        }
      return SkinBinding(entity, guid, skin, keys, Array(keys.size) { index[keys[it]] }, index.size)
    }
  }
}
