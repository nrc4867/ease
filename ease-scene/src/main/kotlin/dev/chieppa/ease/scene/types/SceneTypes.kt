package dev.chieppa.ease.scene.types

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.scene.node.NodeName
import dev.chieppa.ease.scene.node.Parent
import kotlinx.serialization.KSerializer

/**
 * The component types scenes save and spawn, by serial name. Node files name a component by its
 * serial name; a name this build does not know stays in the file and is reported when spawning.
 * [Parent] and [NodeName] are always present, as the node's structure, and so is [LocalTransform];
 * engine modules and games add their own with [Builder.component].
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

  /** Collects the types a [SceneTypes] holds. */
  public class Builder @PublishedApi internal constructor() {
    @PublishedApi internal val types: MutableList<SceneComponentType<*>> = mutableListOf()

    /** Registers the component [type], saved and read through [serializer]. */
    public inline fun <reified T : Component<T>> component(
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
        )
    }
  }

  public companion object {
    /** Serial names of the components that make a node's place in the tree. */
    public val STRUCTURAL: Set<String> =
      setOf(Parent.serializer().descriptor.serialName, NodeName.serializer().descriptor.serialName)

    /** The structural types and [LocalTransform], plus what [register] adds. */
    public fun build(register: Builder.() -> Unit = {}): SceneTypes {
      val builder = Builder()
      builder.component(Parent, Parent.serializer())
      builder.component(NodeName, NodeName.serializer())
      builder.component(LocalTransform, LocalTransform.serializer())
      builder.register()
      return SceneTypes(builder.types)
    }
  }
}
