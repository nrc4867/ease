package dev.chieppa.ease.ecs.replication

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World

/**
 * Copies of one replicated type's components, held as [Any] so stores can keep several types side
 * by side: made by [create], filled from and put back into an entity's component, compared and
 * blended through the type's [serializer]. Nothing here allocates but [create].
 */
internal class TypeCopies<T : Component<T>>(private val serializer: ReplicatedSerializer<T>) {
  /** A new component of this type with every field at its default, to hold a copy of one. */
  fun create(): Any = serializer.create()

  /** Copies [entity]'s component into [into], a copy made by [create]. */
  fun save(world: World, entity: Entity, into: Any) {
    serializer.copy(serializer.componentOf(world, entity), cast(into))
  }

  /** Copies [from], a copy made by [create], into [entity]'s component. */
  fun load(world: World, entity: Entity, from: Any) {
    serializer.copy(cast(from), serializer.componentOf(world, entity))
  }

  /** Sets [entity]'s component [amount] of the way from [from] to [to]: see `interpolate`. */
  fun blend(world: World, entity: Entity, from: Any, to: Any, amount: Float) {
    serializer.interpolate(cast(from), cast(to), amount, serializer.componentOf(world, entity))
  }

  /** Whether [entity]'s component would go on the wire differently from [other]. */
  fun wireDiffers(world: World, entity: Entity, other: Any): Boolean =
    serializer.changedFields(serializer.componentOf(world, entity), cast(other)) != 0

  /** Whether [entity]'s component differs at all from [other]. */
  fun differs(world: World, entity: Entity, other: Any): Boolean =
    serializer.differs(serializer.componentOf(world, entity), cast(other))

  @Suppress("UNCHECKED_CAST") // Callers pass only copies made by create.
  private fun cast(copy: Any): T = copy as T
}
