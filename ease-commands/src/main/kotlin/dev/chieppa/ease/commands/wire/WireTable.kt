package dev.chieppa.ease.commands.wire

import kotlin.reflect.KClass
import kotlinx.serialization.KSerializer

/**
 * The registered subtypes of one polymorphic base, such as every command, each with a wire id: its
 * position when sorted by serial name, so every peer that registers the same types numbers them
 * alike. Lookups allocate nothing.
 */
internal class WireTable(
  /** The base class whose values this table writes. */
  val base: Class<*>,
  subtypes: List<Pair<KClass<*>, KSerializer<*>>>,
) {
  private val sorted = subtypes.sortedBy { it.second.descriptor.serialName }
  private val serializers: Array<KSerializer<*>> = Array(sorted.size) { sorted[it].second }
  private val ids: HashMap<Class<*>, Int> = HashMap()

  init {
    val names = sorted.map { it.second.descriptor.serialName }
    require(names.toSet().size == names.size) {
      "Types registered twice under ${base.name}: $names"
    }
    sorted.forEachIndexed { id, (type, _) -> ids[type.java] = id }
  }

  /** How many subtypes are registered. */
  val size: Int
    get() = serializers.size

  /** The wire id of [value]'s class, or -1 when it is not registered. */
  fun idOf(value: Any): Int = ids[value.javaClass] ?: NONE

  /** The serializer of the subtype with wire id [id], from 0 to [size] exclusive. */
  fun serializerAt(id: Int): KSerializer<*> = serializers[id]

  /** The serial name of the subtype with wire id [id], for messages. */
  fun nameAt(id: Int): String = serializers[id].descriptor.serialName

  companion object {
    /** The id of a class that is not registered. */
    const val NONE: Int = -1
  }
}
