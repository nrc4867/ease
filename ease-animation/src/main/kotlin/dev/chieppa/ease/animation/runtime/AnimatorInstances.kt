package dev.chieppa.ease.animation.runtime

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.animation.assets.AnimatorAsset
import dev.chieppa.ease.animation.graph.AnimatorGraph
import dev.chieppa.ease.animation.scene.Animator
import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.runtime.NodeIndex

/**
 * One world's playing animators by entity id, each bound to its joints, and the graphs compiled
 * from [assets], by animator asset. Graphs are compiled again, and every animator starts over, once
 * the assets change; an asset that does not compile is listed in [refused]. Looking up a bound
 * animator allocates nothing.
 */
internal class AnimatorInstances(private val assets: AnimationAssets?) {
  private var instances = arrayOfNulls<AnimatorInstance>(INITIAL)
  private val graphs = HashMap<Guid, AnimatorGraph>()
  private var seenChanges = assets?.changes ?: 0
  private var graphVersion = 0
  private var tick = 0

  /** Why each animator asset that cannot play cannot, by GUID. */
  val refused = LinkedHashMap<Guid, String>()

  /**
   * Starts a tick: until [of] finds an animator again, [playing] does not answer for it. Every
   * animator starts over when the assets changed since the last tick.
   */
  fun beginTick() {
    tick++
    val changes = assets?.changes ?: 0
    if (changes == seenChanges) return
    seenChanges = changes
    graphs.clear()
    refused.clear()
    graphVersion++
  }

  /**
   * The instance playing [entity]'s animator in [world] this tick, bound to its joints through
   * [index], or null when it is not a live node with an animator asset that plays; then any
   * instance it had is dropped.
   */
  fun of(world: World, entity: Entity, index: NodeIndex): AnimatorInstance? {
    val live = world.contains(entity)
    val guid = if (live) with(world) { entity.getOrNull(Animator) }?.animator?.guid else null
    val key = if (live) with(world) { entity.getOrNull(NodeKey) } else null
    val graph = if (guid == null || key == null) null else graphOf(guid)
    if (graph == null || key == null) {
      if (instances.getOrNull(entity.id)?.entity == entity) instances[entity.id] = null
      return null
    }
    if (entity.id >= instances.size) {
      instances = instances.copyOf(maxOf(entity.id + 1, instances.size * 2))
    }
    val existing = instances[entity.id]
    val instance =
      if (existing != null && existing.plays(entity, graph, graphVersion)) existing
      else AnimatorInstance(entity, key, graph, graphVersion).also { instances[entity.id] = it }
    if (!instance.stillBound(world, index)) instance.bind(world, index)
    instance.seen = tick
    return instance
  }

  /** The instance that played [entity]'s animator this tick, or null when none did. */
  fun playing(entity: Entity): AnimatorInstance? =
    instances.getOrNull(entity.id)?.takeIf { it.entity == entity && it.seen == tick }

  private fun graphOf(guid: Guid): AnimatorGraph? {
    val compiled = graphs[guid]
    val asset = if (compiled == null && guid !in refused) assets?.animator(guid) else null
    return compiled ?: asset?.let { compile(guid, it) }
  }

  private fun compile(guid: Guid, asset: AnimatorAsset) =
    try {
      AnimatorGraph.compile(asset) { assets?.clip(it) }.also { graphs[guid] = it }
    } catch (failure: IllegalArgumentException) {
      refused[guid] = "Animator $guid cannot play: ${failure.message}"
      null
    }

  private companion object {
    const val INITIAL = 64
  }
}
