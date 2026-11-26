package dev.chieppa.ease.animation.runtime

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.animation.graph.AnimatorGraph
import dev.chieppa.ease.animation.graph.Playback
import dev.chieppa.ease.animation.graph.PoseSampler
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.node.NodeName
import dev.chieppa.ease.scene.runtime.NodeIndex
import dev.chieppa.ease.scene.transform.WORLD_TRANSFORM_FLOATS
import dev.chieppa.ease.scene.transform.WorldTransforms
import kotlin.math.sqrt

private const val AXES = 3
private const val ROTATION_W = 6
private const val SCALE_AT = 7

/**
 * One animator entity's [graph] bound to its world: the entity of each joint, found under the
 * animator's [key] by the prefab ids clips name (Entity.NONE while missing), the bone each socket
 * and probe rides on, found by node name under the same key, its [playback] and its last [pose].
 * Binding allocates; playing does not.
 */
internal class AnimatorInstance(
  val entity: Entity,
  val key: NodeKey,
  val graph: AnimatorGraph,
  val graphVersion: Int,
) {
  val playback = Playback(graph)
  val sampler = PoseSampler(graph)
  private val jointCount = graph.joints.size
  val jointKeys: Array<NodeKey> = Array(jointCount) { key.child(graph.joints[it]) }
  val joints: Array<Entity> = Array(jointCount) { Entity.NONE }
  val rest = FloatArray(jointCount * WORLD_TRANSFORM_FLOATS)
  val pose = FloatArray(jointCount * WORLD_TRANSFORM_FLOATS)

  private val sockets = graph.asset.sockets
  val socketBones: Array<Entity> = Array(sockets.size) { Entity.NONE }
  val socketOffsets = FloatArray(sockets.size * AXES)

  private val probes = graph.asset.probes

  /** Each probe's socket index, or -1 when it rides on a bone directly. */
  val probeSockets =
    IntArray(probes.size) { probe -> sockets.indexOfFirst { it.name == probes[probe].at } }
  val probeBones: Array<Entity> = Array(probes.size) { Entity.NONE }

  /** How many nodes the index held when the instance last bound. */
  var boundNodes = -1

  /** The node's world position along x and z last tick, for motion-driven parameters. */
  var lastX = 0f
  var lastZ = 0f
  var moved = false

  /** Whether this tick's pose is worked out. */
  var posed = false

  init {
    for (socket in sockets.indices) {
      socketOffsets[socket * AXES] = sockets[socket].x
      socketOffsets[socket * AXES + 1] = sockets[socket].y
      socketOffsets[socket * AXES + 2] = sockets[socket].z
    }
  }

  /** Whether this instance is [entity]'s, playing [graph] compiled at [version]. */
  fun plays(entity: Entity, graph: AnimatorGraph, version: Int): Boolean =
    this.entity == entity && this.graph === graph && graphVersion == version

  /** Copies each bound joint's own transform into [rest]; a missing joint rests at identity. */
  fun readRest(world: World) {
    for (joint in joints.indices) {
      val node = joints[joint]
      val at = joint * WORLD_TRANSFORM_FLOATS
      if (node != Entity.NONE) {
        WorldTransforms.writeLocal(world, node, rest, at)
      } else {
        rest.fill(0f, at, at + WORLD_TRANSFORM_FLOATS)
        rest[at + ROTATION_W] = 1f
        rest.fill(1f, at + SCALE_AT, at + WORLD_TRANSFORM_FLOATS)
      }
    }
  }

  /**
   * Sets the motion-driven parameters from how far the node moved across the ground since last
   * tick, [seconds] ago, its world position read into [scratch] through [worlds].
   */
  fun driveMotion(world: World, worlds: WorldTransforms?, seconds: Float, scratch: FloatArray) {
    val driven = graph.motionDriven
    if (driven.none { it }) return
    if (worlds != null) worlds.write(entity, scratch, 0)
    else WorldTransforms.writeLocal(world, entity, scratch, 0)
    val dx = scratch[0] - lastX
    val dz = scratch[2] - lastZ
    val speed = if (moved && seconds > 0f) sqrt(dx * dx + dz * dz) / seconds else 0f
    lastX = scratch[0]
    lastZ = scratch[2]
    moved = true
    for (index in driven.indices) if (driven[index]) playback.parameters[index] = speed
  }

  /** Whether every bound joint is still the live node of its key and no node came or went. */
  fun stillBound(world: World, index: NodeIndex): Boolean {
    var bound = index.size == boundNodes && world.contains(entity)
    for (joint in joints.indices) {
      bound = bound && (joints[joint] == Entity.NONE || index[jointKeys[joint]] == joints[joint])
    }
    return bound
  }

  /** Finds every joint, socket bone and probe bone in [world] through [index]. */
  fun bind(world: World, index: NodeIndex) {
    for (joint in joints.indices) joints[joint] = index[jointKeys[joint]]
    if (sockets.isNotEmpty() || probes.isNotEmpty()) {
      val named = namedNodes(world, index)
      for (socket in sockets.indices) {
        socketBones[socket] = named[sockets[socket].bone] ?: Entity.NONE
      }
      for (probe in probes.indices) {
        probeBones[probe] =
          if (probeSockets[probe] >= 0) socketBones[probeSockets[probe]]
          else named[probes[probe].at] ?: Entity.NONE
      }
    }
    boundNodes = index.size
  }

  /** The nodes under this animator's key by name; the first in key order wins a shared name. */
  private fun namedNodes(world: World, index: NodeIndex): Map<String, Entity> {
    val named = HashMap<String, Entity>()
    index.keys
      .filter { it.startsWith(key) && it != key }
      .sortedBy { it.text }
      .forEach { nodeKey ->
        val node = index[nodeKey]
        with(world) { node.getOrNull(NodeName) }?.let { named.putIfAbsent(it.name, node) }
      }
    return named
  }
}
