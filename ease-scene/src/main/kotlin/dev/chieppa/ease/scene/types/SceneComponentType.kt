package dev.chieppa.ease.scene.types

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.EntityUpdateContext
import com.github.quillraven.fleks.World
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * One component type that scenes save and spawn: its Fleks [type], the [serializer] that writes it
 * in node files, and the typed ways to read it from an entity, remove it, and change it through
 * `Replication.write` when it replicates, captured where the type is known.
 */
public class SceneComponentType<T : Component<T>>
@PublishedApi
internal constructor(
  public val type: ComponentType<T>,
  public val serializer: KSerializer<T>,
  private val read: (World, Entity) -> T?,
  private val remove: EntityUpdateContext.(Entity) -> Unit,
  private val replicate: (World, Entity, T?) -> Boolean,
) {
  /** The name the type is saved under, its serializer's serial name. */
  public val serialName: String = serializer.descriptor.serialName

  /** [component]'s fields as node files hold them. */
  public fun encode(component: Component<*>): JsonObject {
    @Suppress("UNCHECKED_CAST") // Callers pass a component of this type, found by its type.
    val typed = component as T
    return JSON.encodeToJsonElement(serializer, typed) as JsonObject
  }

  /** The component [fields] describe. Throws `SerializationException` when they do not fit. */
  public fun decode(fields: JsonObject): T = JSON.decodeFromJsonElement(serializer, fields)

  /** A copy of [component] that shares nothing with it. */
  public fun copyOf(component: Component<*>): T = decode(encode(component))

  /**
   * Copies [value] into [entity]'s component of this type through `Replication.write`, so the
   * change is marked for the next snapshot. Returns false, changing nothing, when the type does not
   * replicate in [world] or [entity] does not carry it.
   */
  public fun writeReplicated(world: World, entity: Entity, value: Component<*>): Boolean {
    if (of(world, entity) == null) return false
    @Suppress("UNCHECKED_CAST") // Callers pass a component of this type, found by its type.
    return replicate(world, entity, value as T)
  }

  /**
   * Marks [entity]'s component of this type as written for the next snapshot, as a new or replaced
   * component is; does nothing when the type does not replicate or is not carried.
   */
  public fun markReplicated(world: World, entity: Entity) {
    if (of(world, entity) != null) replicate(world, entity, null)
  }

  /** [entity]'s component of this type in [world], or null when it carries none. */
  public fun of(world: World, entity: Entity): T? = read(world, entity)

  /** Removes this type from [entity], inside an entity update. */
  public fun removeFrom(context: EntityUpdateContext, entity: Entity): Unit = context.remove(entity)

  override fun toString(): String = serialName

  private companion object {
    /** Every field written, defaults included, so a file never depends on a type's defaults. */
    val JSON = Json {
      encodeDefaults = true
      ignoreUnknownKeys = false
    }
  }
}
