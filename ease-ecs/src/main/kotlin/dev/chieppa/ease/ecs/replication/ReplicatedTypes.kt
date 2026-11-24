package dev.chieppa.ease.ecs.replication

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

/**
 * The replicated types of a world, each with a wire id: their position when sorted by serial name,
 * so every peer that registers the same types numbers them alike.
 */
public class ReplicatedTypes internal constructor(serializers: List<ReplicatedSerializer<*>>) {
  private val byId: List<ReplicatedType<*>>
  private val idByComponentType: IntArray

  init {
    val names = serializers.map { it.name }
    require(names.toSet().size == names.size) { "Replicated types registered twice: $names" }
    require(serializers.size <= MAX_TYPES) { "At most $MAX_TYPES replicated types" }
    val sorted = serializers.sortedBy { it.name }
    byId = sorted.mapIndexed { id, serializer -> typeOf(id, serializer) }
    val maxTypeId = serializers.maxOfOrNull { it.type.id } ?: -1
    idByComponentType = IntArray(maxTypeId + 1) { NONE }
    byId.forEach { idByComponentType[it.serializer.type.id] = it.id }
  }

  /** How many types are registered. */
  public val size: Int
    get() = byId.size

  /** The type with wire id [id], from 0 to [size] exclusive. */
  public operator fun get(id: Int): ReplicatedType<*> = byId[id]

  /** The wire id of [type], or -1 when it does not replicate. */
  public fun idOf(type: ComponentType<*>): Int = idByComponentType.getOrElse(type.id) { NONE }

  /** Whether [type] replicates in this world. */
  public operator fun contains(type: ComponentType<*>): Boolean = idOf(type) != NONE

  private fun <T : Component<T>> typeOf(
    id: Int,
    serializer: ReplicatedSerializer<T>,
  ): ReplicatedType<T> = ReplicatedType(id, serializer)

  public companion object {
    /** The most replicated types a world may register: a type id is one byte on the wire. */
    public const val MAX_TYPES: Int = 256

    /** Bits a type id takes on the wire. */
    public const val ID_BITS: Int = 8

    private const val NONE = -1
  }
}
