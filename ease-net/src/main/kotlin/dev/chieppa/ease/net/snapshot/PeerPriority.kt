package dev.chieppa.ease.net.snapshot

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.net.relevancy.PriorityRule
import dev.chieppa.ease.net.relevancy.Viewer

private const val INITIAL_ENTITIES = 64

/**
 * The order one peer's snapshot takes its entities in. Each entity with changes the peer lacks is
 * [offer]ed with its [PriorityRule] score plus what it built up while left out; [sort] puts the
 * highest first. An entity a snapshot carries starts again from 0 and one left out keeps its total,
 * so an entity left out long enough rises past any score. It allocates only as the world grows.
 */
internal class PeerPriority(
  private val world: World,
  private val viewer: Viewer,
  private val motion: EntityMotion,
  private val rule: PriorityRule,
) {
  private var builtUp = FloatArray(INITIAL_ENTITIES)
  private var ids = IntArray(INITIAL_ENTITIES)
  private var keys = FloatArray(INITIAL_ENTITIES)

  /** How many entities were offered since [clear]. */
  var size: Int = 0
    private set

  /** Forgets the entities offered for the last snapshot. */
  fun clear() {
    size = 0
  }

  /** Forgets what [entity] built up, as when it enters the peer's view. */
  fun reset(entity: Entity) {
    if (entity.id < builtUp.size) builtUp[entity.id] = 0f
  }

  /** Offers [entity], which has changes the peer lacks, for the next snapshot. */
  fun offer(entity: Entity) {
    val id = entity.id
    if (id >= builtUp.size) builtUp = builtUp.copyOf(maxOf(id + 1, builtUp.size * 2))
    if (size == ids.size) {
      ids = ids.copyOf(size * 2)
      keys = keys.copyOf(size * 2)
    }
    val score = rule.priority(world, entity, viewer, motion.speedOf(entity)).coerceAtLeast(0f)
    ids[size] = id
    keys[size] = builtUp[id] + score
    size++
  }

  /** Puts the offered entities in order, highest first. */
  fun sort() {
    HeapSort.descending(ids, keys, size)
  }

  /** The entity id at [index] of the order. */
  fun idAt(index: Int): Int = ids[index]

  /** Records whether the snapshot [carried] the entity at [index] of the order. */
  fun settle(index: Int, carried: Boolean) {
    builtUp[ids[index]] = if (carried) 0f else keys[index]
  }
}
