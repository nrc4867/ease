package dev.chieppa.ease.scene.types

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.commands.CommandRegistry
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.replication.ReplicatedSerializer
import dev.chieppa.ease.ecs.replication.replication
import dev.chieppa.ease.scene.node.NodeName
import dev.chieppa.ease.scene.node.Parent
import kotlinx.serialization.KSerializer

/**
 * The component types scenes save and spawn, by serial name. Node files name a component by its
 * serial name; a name this build does not know stays in the file and is reported when spawning.
 * [Parent] and [NodeName] are always present, as the node's structure, and so is [LocalTransform];
 * engine modules and games add their own with [Builder.component], which registers each with a
 * world's codec too, so a scene component type is registered in one place.
 */
public class SceneTypes private constructor(types: List<SceneComponentType<*>>) {
  private val byName = types.associateBy { it.serialName }

  /** Every type, structure included, in registration order. */
  public val all: List<SceneComponentType<*>> = types

  /** The types a node file's `components` may hold: all but the structural ones. */
  public val saved: List<SceneComponentType<*>> = types.filter { it.serialName !in STRUCTURAL }

  /** The type saved as [serialName], or null when there is none. */
  public operator fun get(serialName: String): SceneComponentType<*>? = byName[serialName]

  /** The type of [component], or null when scenes do not know it. */
  public fun of(component: Component<*>): SceneComponentType<*>? {
    val type = component.type()
    return all.firstOrNull { it.type === type }
  }

  /**
   * Collects the types a [SceneTypes] holds. Built over a world's [registry], it registers each
   * type it is given with that registry as well, so the codec reads and writes it.
   */
  public class Builder
  @PublishedApi
  internal constructor(@PublishedApi internal val registry: CommandRegistry?) {
    @PublishedApi internal val types: MutableList<SceneComponentType<*>> = mutableListOf()

    /**
     * Registers the component [type], saved and read through [serializer], with the codec's
     * registry too when there is one.
     */
    public inline fun <reified T : Component<T>> component(
      type: ComponentType<T>,
      serializer: KSerializer<T>,
    ) {
      add(type, serializer)
      registry?.component(T::class, serializer)
    }

    /** Adds the component [type] to the scene types alone, for a type the codec already has. */
    @PublishedApi
    internal inline fun <reified T : Component<T>> add(
      type: ComponentType<T>,
      serializer: KSerializer<T>,
    ) {
      val name = serializer.descriptor.serialName
      require(types.none { it.serialName == name }) { "Component $name is registered twice" }
      types +=
        SceneComponentType(
          type,
          serializer,
          read = { world, entity -> with(world) { entity.getOrNull(type) } },
          remove = { entity -> entity -= type },
          replicate = { world, entity, value ->
            val wire = world.replication.types
            val id = wire.idOf(type)
            if (id >= 0) {
              @Suppress("UNCHECKED_CAST") // The wire type registered for this component type.
              val replicated = wire[id].serializer as ReplicatedSerializer<T>
              world.replication.write(entity, type) { value?.let { replicated.copy(it, this) } }
            }
            id >= 0
          },
        )
    }
  }

  public companion object {
    /** Serial names of the components that make a node's place in the tree. */
    public val STRUCTURAL: Set<String> =
      setOf(Parent.serializer().descriptor.serialName, NodeName.serializer().descriptor.serialName)

    /**
     * The structural types and [LocalTransform], plus what [register] adds, which is registered
     * with [registry] too when one is given. The structural types and [LocalTransform] are the
     * codec's own: `registerSceneCommands` and the built-ins register them there.
     */
    public fun build(
      registry: CommandRegistry? = null,
      register: Builder.() -> Unit = {},
    ): SceneTypes {
      val builder = Builder(registry)
      builder.add(Parent, Parent.serializer())
      builder.add(NodeName, NodeName.serializer())
      builder.add(LocalTransform, LocalTransform.serializer())
      builder.register()
      return SceneTypes(builder.types)
    }
  }
}
