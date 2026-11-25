package dev.chieppa.ease.net.view

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Family
import com.github.quillraven.fleks.World
import dev.chieppa.ease.ecs.replication.Authority
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.ecs.replication.replication
import dev.chieppa.ease.net.channel.ChannelKind
import dev.chieppa.ease.net.channel.Connection
import dev.chieppa.ease.net.relevancy.Relevance
import dev.chieppa.ease.net.relevancy.Viewer
import dev.chieppa.ease.net.snapshot.PeerSnapshots
import dev.chieppa.ease.net.snapshot.ViewMembers

private const val INITIAL_ENTITIES = 64

/** The network id of no entity: an entity id with nothing in view. */
private const val NOT_IN_VIEW = 0L

/** A tick before every tick: an entity in view that is still relevant. */
private const val STILL_RELEVANT = Long.MIN_VALUE

/**
 * Which of the host's entities one peer holds, kept each tick by [update]. An entity enters the
 * view, with a spawn, on the tick it becomes relevant to the peer's [viewer]. It leaves, with a
 * despawn, at once when a filter says `NEVER`, the host destroys it or stops holding it; when it is
 * only `DISTANT`, it leaves on the tick it has been so for the grace ticks, at once for 0. An
 * entity whose owner or set of replicated components changed leaves and enters again in the same
 * tick, so the peer's copy and the fields it may hold follow. A tick in which nothing enters or
 * leaves allocates nothing.
 */
internal class PeerView(
  private val world: World,
  private val entities: Family,
  private val viewer: Viewer,
  private val settings: ViewSettings,
  private val snapshots: PeerSnapshots,
  private val spawns: SpawnWriter,
) : ViewMembers {
  private val replication = world.replication
  private val filters = settings.filters.toTypedArray()
  private var inView = LongArray(INITIAL_ENTITIES)
  private var lostAt = LongArray(INITIAL_ENTITIES)
  private var owners = IntArray(INITIAL_ENTITIES)
  private var seenAt = LongArray(INITIAL_ENTITIES)
  private var positions = IntArray(INITIAL_ENTITIES)
  private var members = IntArray(INITIAL_ENTITIES)

  /** How many spawns and despawns the peer has been sent. */
  private var sequence = 0L
  private var spawnedBy = LongArray(INITIAL_ENTITIES)

  override var count: Int = 0
    private set

  override fun memberAt(index: Int): Int = members[index]

  override fun networkIdOf(entityId: Int): Long = inView[entityId]

  override fun spawnOf(entityId: Int): Long = spawnedBy[entityId]

  override fun isOwner(entityId: Int): Boolean = owners[entityId] == viewer.peer.value

  /**
   * Brings the view in line with the world at [tick], sending spawns and despawns on [connection].
   */
  fun update(tick: Long, connection: Connection) {
    entities.forEach { entity ->
      if (replication.hasAuthority(entity)) visit(entity, tick, connection)
    }
    for (index in count - 1 downTo 0) {
      val id = members[index]
      if (seenAt[id] != tick) leave(id, connection)
    }
  }

  private fun visit(entity: Entity, tick: Long, connection: Connection) {
    val id = entity.id
    if (id >= inView.size) grow(id)
    val networkId = replication.networkIdOf(entity).value
    if (inView[id] != NOT_IN_VIEW && inView[id] != networkId) leave(id, connection)
    seenAt[id] = tick
    val owner = with(world) { entity[Authority].owner }
    val verdict = verdictOf(entity, owner)
    val relevant = verdict == Relevance.ALWAYS || verdict == Relevance.PASS
    var member = inView[id] != NOT_IN_VIEW
    if (member && (owners[id] != owner.value || !snapshots.holdsAll(entity, networkId))) {
      leave(id, connection)
      member = false
    }
    when {
      !member -> if (relevant) enter(entity, networkId, owner, connection)
      relevant -> lostAt[id] = STILL_RELEVANT
      verdict == Relevance.NEVER -> leave(id, connection)
      else -> fade(id, tick, connection)
    }
  }

  /**
   * Counts the grace of the entity at [id], out of range at [tick], and takes it out once spent.
   */
  private fun fade(id: Int, tick: Long, connection: Connection) {
    if (lostAt[id] == STILL_RELEVANT) lostAt[id] = tick
    if (tick - lostAt[id] >= settings.graceTicks) leave(id, connection)
  }

  /** What the filters together say about [entity], owned by [owner]: see `RelevancyFilter`. */
  private fun verdictOf(entity: Entity, owner: PeerId): Relevance {
    var always = owner == viewer.peer
    var never = false
    var distant = false
    for (filter in filters) {
      when (filter.relevance(world, entity, viewer)) {
        Relevance.ALWAYS -> always = true
        Relevance.NEVER -> never = true
        Relevance.DISTANT -> distant = true
        Relevance.PASS -> Unit
      }
    }
    return when {
      always -> Relevance.ALWAYS
      never -> Relevance.NEVER
      distant -> Relevance.DISTANT
      else -> Relevance.PASS
    }
  }

  private fun enter(entity: Entity, networkId: Long, owner: PeerId, connection: Connection) {
    val length = spawns.spawn(entity, networkId, owner, toOwner = owner == viewer.peer)
    if (length == SpawnWriter.NO_FIT) return
    connection.send(ChannelKind.RELIABLE_ORDERED, spawns.buffer, 0, length)
    sequence++
    val id = entity.id
    inView[id] = networkId
    spawnedBy[id] = sequence
    lostAt[id] = STILL_RELEVANT
    owners[id] = owner.value
    positions[id] = count
    if (count == members.size) members = members.copyOf(count * 2)
    members[count++] = id
    snapshots.know(entity, networkId)
  }

  private fun leave(id: Int, connection: Connection) {
    val networkId = inView[id]
    val length = spawns.despawn(networkId)
    connection.send(ChannelKind.RELIABLE_ORDERED, spawns.buffer, 0, length)
    sequence++
    snapshots.forget(id, networkId)
    inView[id] = NOT_IN_VIEW
    val position = positions[id]
    val last = members[--count]
    members[position] = last
    positions[last] = position
  }

  private fun grow(id: Int) {
    val size = maxOf(id + 1, inView.size * 2)
    inView = inView.copyOf(size)
    spawnedBy = spawnedBy.copyOf(size)
    lostAt = lostAt.copyOf(size)
    owners = owners.copyOf(size)
    seenAt = seenAt.copyOf(size)
    positions = positions.copyOf(size)
  }
}
