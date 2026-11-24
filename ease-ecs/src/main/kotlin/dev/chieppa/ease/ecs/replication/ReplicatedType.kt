package dev.chieppa.ease.ecs.replication

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World

/**
 * One registered replicated type in a world: its wire [id], its [serializer], a scratch component
 * that checks received state without touching the world, and, for the debug write guard, the last
 * value each entity's component was known to hold.
 */
public class ReplicatedType<T : Component<T>>
internal constructor(public val id: Int, public val serializer: ReplicatedSerializer<T>) {
  private val scratch: T = serializer.create()
  private var known: Array<Any?> = arrayOfNulls(INITIAL_ENTITIES)
  private var seen: Array<Any?> = arrayOfNulls(INITIAL_ENTITIES)

  /** Whether [entity] carries this type. */
  public fun isOn(world: World, entity: Entity): Boolean =
    with(world) { entity.has(serializer.type) }

  /** Writes [entity]'s component with the change mask [fields]. */
  public fun writeFrom(world: World, entity: Entity, writer: BitWriter, fields: Int) {
    val component = serializer.componentOf(world, entity)
    serializer.write(writer, component, fields)
  }

  /** Reads one component's state into the scratch component, so nothing in a world changes. */
  internal fun check(reader: BitReader) {
    serializer.read(reader, scratch)
  }

  /** Reads one component's state into [entity]'s component. */
  internal fun readInto(world: World, entity: Entity, reader: BitReader) {
    serializer.read(reader, serializer.componentOf(world, entity))
  }

  /** Records [entity]'s component as it is now, for [unchangedSinceKnown]. */
  internal fun remember(world: World, entity: Entity) {
    val component = serializer.componentOf(world, entity)
    val index = entity.id
    if (index >= known.size) grow(index)
    @Suppress("UNCHECKED_CAST") // Only this type's components are stored here.
    val copy = (known[index] as T?) ?: serializer.create().also { known[index] = it }
    serializer.copy(component, copy)
    seen[index] = component
  }

  /**
   * Whether [entity]'s component holds what [remember] last recorded. A component not seen before,
   * new on the entity or on a reused entity id, is recorded now and counts as unchanged.
   */
  internal fun unchangedSinceKnown(world: World, entity: Entity): Boolean {
    val component = serializer.componentOf(world, entity)
    val index = entity.id
    if (index >= seen.size || seen[index] !== component) {
      remember(world, entity)
      return true
    }
    @Suppress("UNCHECKED_CAST") // Only this type's components are stored here.
    val copy = known[index] as T
    return !serializer.differs(component, copy)
  }

  private fun grow(index: Int) {
    val size = maxOf(index + 1, known.size * 2)
    known = known.copyOf(size)
    seen = seen.copyOf(size)
  }

  private companion object {
    const val INITIAL_ENTITIES = 64
  }
}
