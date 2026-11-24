package dev.chieppa.ease.net.snapshot

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.ecs.replication.ReplicatedFields
import dev.chieppa.ease.ecs.replication.ReplicatedSerializer

private const val INITIAL_ENTITIES = 64

/**
 * The value each entity's component of one replicated type held at the last snapshot the host
 * built, to find which fields changed since. It allocates only when an entity id is first seen.
 */
internal class TypeBaseline<T : Component<T>>(private val serializer: ReplicatedSerializer<T>) {
  private var copies: Array<Any?> = arrayOfNulls(INITIAL_ENTITIES)

  /** The mask naming every field. */
  val allFields: Int = ReplicatedFields.allFields(serializer.fieldCount)

  /**
   * The fields of [entity]'s component whose wire value differs from the last one recorded, which
   * it then records; every field when [fresh], for an entity not seen under this id before.
   */
  fun changed(world: World, entity: Entity, fresh: Boolean): Int {
    val current = serializer.componentOf(world, entity)
    val index = entity.id
    if (index >= copies.size) copies = copies.copyOf(maxOf(index + 1, copies.size * 2))
    @Suppress("UNCHECKED_CAST") // Only this type's components are stored here.
    val copy = (copies[index] as T?) ?: serializer.create().also { copies[index] = it }
    val fields = if (fresh) allFields else serializer.changedFields(current, copy)
    if (fields != 0) serializer.copy(current, copy)
    return fields
  }

  companion object {
    /** A baseline for [serializer]'s type. */
    fun <T : Component<T>> of(serializer: ReplicatedSerializer<T>): TypeBaseline<T> =
      TypeBaseline(serializer)
  }
}
