package dev.chieppa.ease.animation.runtime

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.scene.transform.PosedTransforms
import dev.chieppa.ease.scene.transform.TRANSFORM_ROTATION_W
import dev.chieppa.ease.scene.transform.TRANSFORM_SCALE_AT
import dev.chieppa.ease.scene.transform.TransformMath
import dev.chieppa.ease.scene.transform.WORLD_TRANSFORM_FLOATS
import dev.chieppa.ease.scene.transform.WorldTransforms

private const val NOT_POSED = -1

/**
 * The poses the animators of one world give `WorldTransforms`: each joint entity placed by its
 * animator's pose in place of its `LocalTransform`, and each socket follower placed at its socket.
 * Lookups are arrays indexed by entity id, refilled every tick from the live animators, so a joint
 * whose animator is gone falls back to its own transform; nothing allocates once the arrays fit.
 */
internal class AnimatedPoses(private val world: World) : PosedTransforms {
  private var owners = arrayOfNulls<AnimatorInstance>(INITIAL)
  private var slots = IntArray(INITIAL) { NOT_POSED }
  private var followed = arrayOfNulls<AnimatorInstance>(INITIAL)
  private var followedSockets = IntArray(INITIAL) { NOT_POSED }
  private var marked = IntArray(INITIAL)
  private var markedCount = 0
  private val socket = FloatArray(WORLD_TRANSFORM_FLOATS)
  private val own = FloatArray(WORLD_TRANSFORM_FLOATS)
  private val math = TransformMath()
  var worlds: WorldTransforms? = null

  /** Forgets every pose and follower set last tick. */
  fun clear() {
    for (index in 0 until markedCount) {
      val id = marked[index]
      owners[id] = null
      slots[id] = NOT_POSED
      followed[id] = null
      followedSockets[id] = NOT_POSED
    }
    markedCount = 0
  }

  /** Poses [instance]'s bound joints from its pose. */
  fun add(instance: AnimatorInstance) {
    val joints = instance.joints
    for (joint in joints.indices) {
      val entity = joints[joint]
      if (entity == Entity.NONE) continue
      mark(entity.id)
      owners[entity.id] = instance
      slots[entity.id] = joint
    }
  }

  /** Places [follower] at socket [socket] of [instance]. */
  fun follow(follower: Entity, instance: AnimatorInstance, socket: Int) {
    mark(follower.id)
    followed[follower.id] = instance
    followedSockets[follower.id] = socket
  }

  override fun write(entity: Entity, dst: FloatArray, at: Int): Int {
    val id = entity.id
    val owner = owners.getOrNull(id)
    val follows = followed.getOrNull(id)
    return when {
      owner != null && owner.posed -> {
        val from = slots[id] * WORLD_TRANSFORM_FLOATS
        System.arraycopy(owner.pose, from, dst, at, WORLD_TRANSFORM_FLOATS)
        PosedTransforms.LOCAL
      }
      follows != null && socketWorld(follows, followedSockets[id], socket) -> {
        WorldTransforms.writeLocal(world, entity, own, 0)
        math.compose(socket, 0, own, 0, dst, at)
        PosedTransforms.WORLD
      }
      else -> PosedTransforms.UNPOSED
    }
  }

  /**
   * Writes socket [index] of [instance]'s world transform into [dst]: its bone's world transform
   * moved by the socket's offset in the bone's space. Returns false when the bone is not bound.
   */
  fun socketWorld(instance: AnimatorInstance, index: Int, dst: FloatArray): Boolean {
    val bone = instance.socketBones[index]
    val transforms = worlds
    if (bone == Entity.NONE || transforms == null) return false
    transforms.write(bone, dst, 0)
    own.fill(0f)
    own[TRANSFORM_ROTATION_W] = 1f
    for (axis in 0 until AXES) {
      own[axis] = instance.socketOffsets[index * AXES + axis]
      own[TRANSFORM_SCALE_AT + axis] = 1f
    }
    math.compose(dst, 0, own, 0, dst, 0)
    return true
  }

  private fun mark(id: Int) {
    ensureCapacity(id + 1)
    if (markedCount == marked.size) marked = marked.copyOf(marked.size * 2)
    marked[markedCount++] = id
  }

  private fun ensureCapacity(needed: Int) {
    if (needed <= slots.size) return
    val grown = maxOf(needed, slots.size * 2)
    val old = slots.size
    owners = owners.copyOf(grown)
    followed = followed.copyOf(grown)
    slots = slots.copyOf(grown).also { it.fill(NOT_POSED, old) }
    followedSockets = followedSockets.copyOf(grown).also { it.fill(NOT_POSED, old) }
  }

  private companion object {
    const val INITIAL = 256
    const val AXES = 3
  }
}
