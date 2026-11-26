package dev.chieppa.ease.animation.runtime

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.animation.events.AnimationEvents
import dev.chieppa.ease.animation.probe.ProbeRunner
import dev.chieppa.ease.animation.scene.Animator
import dev.chieppa.ease.animation.scene.SocketFollower
import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.runtime.SceneNodes
import dev.chieppa.ease.scene.transform.WORLD_TRANSFORM_FLOATS
import dev.chieppa.ease.scene.transform.WorldTransforms

/**
 * Plays every [Animator] of a world, in `Phase.ANIMATION`: each tick it sets motion-driven
 * parameters from how far the node moved, advances playback (transitions, crossfades, blends),
 * samples the pose over the joints' own transforms, and gives the poses to the world's
 * `WorldTransforms`, so bones, everything under them and every [SocketFollower] follow; then it
 * reports the clip events playback passed and runs the probes listening for them, through [events].
 * Joints keep their `LocalTransform`s: the pose is derived state, worked out by every peer from the
 * parameters it has and never replicated.
 *
 * Assets come from [assets]; an animator whose asset is missing or wrong is listed in [problems]
 * and left in its rest pose. Binding an animator to its joints allocates; a steady tick does not.
 */
public class AnimatorSystem(
  private val assets: AnimationAssets? = null,
  public val events: AnimationEvents = AnimationEvents(),
) : IteratingSystem(family { all(Animator, NodeKey) }) {
  private val instances = AnimatorInstances(assets)
  private var active = arrayOfNulls<AnimatorInstance>(INITIAL)
  private var activeCount = 0
  private val followers = family { all(SocketFollower) }
  private val position = FloatArray(WORLD_TRANSFORM_FLOATS)
  private lateinit var poses: AnimatedPoses
  private lateinit var dispatch: EventDispatch
  private var nodes: SceneNodes? = null
  private var worlds: WorldTransforms? = null
  private var looked = false

  /** Why each animator asset that cannot play cannot, one line each, and the assets' own. */
  public val problems: Collection<String>
    get() = instances.refused.values + (assets?.problems ?: emptyList())

  override fun onInit() {
    poses = AnimatedPoses(world)
    dispatch = EventDispatch(events, ProbeRunner(world, events))
  }

  override fun onTick() {
    if (!looked) {
      looked = true
      nodes = world.systemOrNull<SceneNodes>()
      worlds = world.systemOrNull<WorldTransforms>()?.also { it.posed = poses }
      poses.worlds = worlds
    }
    instances.beginTick()
    worlds?.invalidate()
    activeCount = 0
    poses.clear()
    super.onTick()
    followers.forEach { follow(it) }
    worlds?.invalidate()
    for (index in 0 until activeCount) dispatch.fire(checkNotNull(active[index]), poses, worlds)
  }

  override fun onTickEntity(entity: Entity) {
    val index = sceneNodes()?.index ?: return
    val instance = instances.of(world, entity, index) ?: return
    instance.readRest(world)
    instance.driveMotion(world, worlds, deltaTime, position)
    instance.playback.advance(deltaTime)
    instance.sampler.sample(instance.playback, instance.rest, instance.pose)
    instance.posed = true
    poses.add(instance)
    if (activeCount == active.size) active = active.copyOf(active.size * 2)
    active[activeCount++] = instance
  }

  /** The world's scene nodes; looked up once, since finding a system searches them all. */
  private fun sceneNodes(): SceneNodes? {
    if (!looked) nodes = world.systemOrNull<SceneNodes>()
    return nodes
  }

  /** Places [follower] at its socket when its animator plays and has that socket. */
  private fun follow(follower: Entity) {
    val socket = with(world) { follower[SocketFollower] }
    val animator = sceneNodes()?.index?.get(socket.animator.key) ?: Entity.NONE
    val instance = instances.playing(animator) ?: return
    val sockets = instance.graph.asset.sockets
    for (index in 0 until sockets.size) {
      if (sockets[index].name == socket.socket) poses.follow(follower, instance, index)
    }
  }

  /**
   * Sets [entity]'s animator parameter [name] to [value]; returns false when it has no animator
   * that plays or no such parameter. A parameter driven by motion is set again next tick.
   */
  public fun setParameter(entity: Entity, name: String, value: Float): Boolean {
    val index = sceneNodes()?.index
    val instance = if (index == null) null else instances.of(world, entity, index)
    val parameter = instance?.graph?.parameterIndex(name) ?: -1
    if (parameter >= 0) checkNotNull(instance).playback.parameters[parameter] = value
    return parameter >= 0
  }

  /** [entity]'s animator parameter [name], or NaN when it has no such parameter playing. */
  public fun parameter(entity: Entity, name: String): Float {
    val instance = instances.playing(entity) ?: return Float.NaN
    val index = instance.graph.parameterIndex(name)
    return if (index < 0) Float.NaN else instance.playback.parameters[index]
  }

  /** The name of the state [entity]'s animator plays or fades to, or null when none plays. */
  public fun stateOf(entity: Entity): String? =
    instances.playing(entity)?.let { it.graph.states[it.playback.state].name }

  /** The share of [entity]'s pose that clip [clip] made at the last tick, 0 to 1. */
  public fun clipWeight(entity: Entity, clip: Guid): Float {
    val instance = instances.playing(entity) ?: return 0f
    val clips = instance.graph.clips
    var weight = 0f
    for (index in clips.indices) {
      if (clips[index].guid == clip) weight += instance.playback.clipWeights[index]
    }
    return weight
  }

  private companion object {
    const val INITIAL = 64
  }
}
