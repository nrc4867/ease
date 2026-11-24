package dev.chieppa.ease.commands

import com.github.quillraven.fleks.Component
import dev.chieppa.ease.ecs.replication.ReplicatedSerializer
import kotlin.reflect.KClass
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * The command and component types a [WorldCodec] can read and write, and the components that
 * replicate. Engine modules and games register their own; the built-ins are always present.
 */
public class CommandRegistry internal constructor() {
  private val commands = mutableListOf<Registration<out Command>>()
  private val components = mutableListOf<Registration<out Component<*>>>()
  private val replicated = mutableListOf<ReplicatedSerializer<*>>()

  /** Registers a command type with its generated serializer. */
  public fun <T : Command> command(type: KClass<T>, serializer: KSerializer<T>) {
    commands += Registration(type, serializer)
  }

  /** Registers a component type with its generated serializer. */
  public fun <T : Component<*>> component(type: KClass<T>, serializer: KSerializer<T>) {
    components += Registration(type, serializer)
  }

  /**
   * Registers a replicated component type with the serializer KSP generated for it, or one a
   * `ReplicatedSchema` made. Register its full-precision serializer with [component] as well.
   */
  public fun replicated(serializer: ReplicatedSerializer<*>) {
    replicated += serializer
  }

  internal fun replicatedSerializers(): List<ReplicatedSerializer<*>> = replicated.toList()

  internal fun commandDescriptors(): List<SerialDescriptor> = commands.map {
    it.serializer.descriptor
  }

  internal fun componentDescriptors(): List<SerialDescriptor> = components.map {
    it.serializer.descriptor
  }

  internal fun module(): SerializersModule = SerializersModule {
    polymorphic(Command::class) { commands.forEach { it.addTo(this) } }
    polymorphic(Component::class) { components.forEach { it.addTo(this) } }
  }

  private class Registration<T : Any>(val type: KClass<T>, val serializer: KSerializer<T>) {
    fun <B : Any> addTo(builder: PolymorphicModuleBuilder<B>) {
      @Suppress("UNCHECKED_CAST") // Registration only accepts subtypes of the builder's base.
      (builder as PolymorphicModuleBuilder<Any>).subclass(type, serializer)
    }
  }
}
