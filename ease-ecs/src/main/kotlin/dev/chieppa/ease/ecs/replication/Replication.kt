package dev.chieppa.ease.ecs.replication

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World

/** The name a world's [Replication] is injected under. */
public const val REPLICATION_INJECTABLE: String = "ease.replication"

/** The replication state of [this] world, which every phased world has. */
public val World.replication: Replication
  get() = inject(REPLICATION_INJECTABLE)

/**
 * One world's part in replication: the peer it runs as, the network ids of its entities, its
 * replicated types, and which replicated components changed since [clearDirty]. Systems take it
 * once when they are built; commands read it through [World.replication].
 */
public class Replication internal constructor(settings: ReplicationSettings) {
  /** The peer this world runs as. */
  public val localPeer: PeerId = settings.localPeer

  /** Whether writes are guarded as in a debug build: see [write]. */
  public val debugChecks: Boolean = settings.debugChecks

  /** The network id of every live entity. */
  public val entities: NetworkEntities = NetworkEntities()

  /** The component types that replicate in this world. */
  public val types: ReplicatedTypes = ReplicatedTypes(settings.serializers)

  private val dirty = Array(types.size) { EntityBits() }

  @PublishedApi internal lateinit var world: World

  internal fun attach(world: World) {
    this.world = world
  }

  /** Whether this world holds authority over [entity], and so may change its replicated state. */
  public fun hasAuthority(entity: Entity): Boolean {
    val authority = with(world) { entity.getOrNull(Authority) } ?: return false
    return authority.holder == localPeer
  }

  /** [entity]'s network id. */
  public fun networkIdOf(entity: Entity): NetworkId = with(world) { entity[NetworkIdentity].id }

  /**
   * Changes [entity]'s component of [type] in [change] and, when [type] replicates, marks it
   * changed for the next snapshot. Without authority over the entity, a debug build throws
   * [AuthorityViolation]; otherwise the change is skipped and this returns false.
   */
  public inline fun <reified T : Component<T>> write(
    entity: Entity,
    type: ComponentType<T>,
    change: T.() -> Unit,
  ): Boolean {
    val id = types.idOf(type)
    if (!hasAuthority(entity)) {
      refuseWrite(entity, id)
      return false
    }
    with(world) { entity[type] }.change()
    if (id >= 0) written(entity, id)
    return true
  }

  /**
   * Whether [entity]'s component of [type] changed through [write] since [clearDirty]; always false
   * for a type that does not replicate.
   */
  public fun isDirty(entity: Entity, type: ComponentType<*>): Boolean {
    val id = types.idOf(type)
    return id >= 0 && dirty[id][entity.id]
  }

  /** Forgets every change [write] marked, once a snapshot has carried them. */
  public fun clearDirty() {
    dirty.forEach { it.clearAll() }
  }

  @PublishedApi
  internal fun replicatedIdOf(type: ComponentType<*>): Int {
    val id = types.idOf(type)
    require(id >= 0) { "Component type ${type.id} does not replicate in this world" }
    return id
  }

  @PublishedApi
  internal fun refuseWrite(entity: Entity, typeId: Int) {
    if (!debugChecks) return
    val name = if (typeId >= 0) types[typeId].serializer.name else "a component"
    throw AuthorityViolation(
      "${describe(entity)} was written without authority to $name: " +
        "${holderOf(entity)} holds it and this world is $localPeer"
    )
  }

  @PublishedApi
  internal fun written(entity: Entity, typeId: Int) {
    dirty[typeId].set(entity.id)
    if (debugChecks) types[typeId].remember(world, entity)
  }

  /** Records a received change to [entity]'s component, which is not this world's own change. */
  internal fun received(entity: Entity, typeId: Int) {
    if (debugChecks) types[typeId].remember(world, entity)
  }
}
