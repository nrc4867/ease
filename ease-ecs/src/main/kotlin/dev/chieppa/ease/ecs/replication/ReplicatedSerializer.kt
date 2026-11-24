package dev.chieppa.ease.ecs.replication

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World

/**
 * Writes and reads one replicated component type as deltas: a change mask with a bit per field,
 * then only the fields the mask names. KSP writes one for every `@Replicated` class; a
 * [ReplicatedSchema] makes one for a component described at runtime. Nothing here allocates.
 */
public interface ReplicatedSerializer<T : Component<T>> {
  /** The component's serial name, the same one its full-precision form uses. */
  public val name: String

  /** The component type it serializes. */
  public val type: ComponentType<T>

  /** How many fields replicate, up to [ReplicatedFields.MAX_FIELDS]. */
  public val fieldCount: Int

  /** The fields replicas blend between snapshots. */
  public val interpolatedFields: Int

  /** The fields the entity's owner receives. */
  public val ownerFields: Int

  /** The fields every other peer receives. */
  public val othersFields: Int

  /** A component with every field at its default. */
  public fun create(): T

  /** [entity]'s component of this type in [world]; the entity must carry one. */
  public fun componentOf(world: World, entity: Entity): T

  /** The fields whose wire value differs between [value] and [baseline], after quantization. */
  public fun changedFields(value: T, baseline: T): Int

  /** Whether any field of [a] differs from [b] at all, bit for bit. */
  public fun differs(a: T, b: T): Boolean

  /** Writes [fields] as the change mask, then the value of each field it names. */
  public fun write(writer: BitWriter, value: T, fields: Int)

  /**
   * Reads a change mask and the fields it names into [target]; fields it does not name keep the
   * baseline [target] holds. A problem is left in [reader]'s status.
   */
  public fun read(reader: BitReader, target: T)

  /** Copies every field of [from] into [to]. */
  public fun copy(from: T, to: T)
}
