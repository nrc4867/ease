package dev.chieppa.ease.commands

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.Snapshot
import com.github.quillraven.fleks.World
import dev.chieppa.ease.commands.builtin.registerBuiltins
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.Json

/**
 * Reads and writes commands and world state: JSON for agents, scripts and plugins, a compact binary
 * form for the command log and the network. Both come from the same `@Serializable` declarations.
 */
@OptIn(ExperimentalSerializationApi::class)
public class WorldCodec internal constructor(registry: CommandRegistry) {
  private val module = registry.module()
  private val json = Json {
    serializersModule = module
    classDiscriminator = TYPE_KEY
    // Every field is written, defaults included, so a reader never has to know a type's defaults.
    encodeDefaults = true
  }
  private val binary = Cbor { serializersModule = module }
  private val commandSerializer = PolymorphicSerializer(Command::class)
  private val worldSerializer = ListSerializer(EntityState.serializer())

  /**
   * The descriptor of every command type this codec reads and writes, built-ins first, as the
   * serialization plugin generated them: serial name, kind and fields.
   */
  public val commandDescriptors: List<SerialDescriptor> = registry.commandDescriptors()

  /** The descriptor of every component type this codec reads and writes, built-ins first. */
  public val componentDescriptors: List<SerialDescriptor> = registry.componentDescriptors()

  /** [command] as JSON, its type under the `type` key. */
  public fun commandToJson(command: Command): String =
    json.encodeToString(commandSerializer, command)

  /** The command [text] holds. */
  public fun commandFromJson(text: String): Command = json.decodeFromString(commandSerializer, text)

  /** [command] in the binary form. */
  public fun commandToBinary(command: Command): ByteArray =
    binary.encodeToByteArray(commandSerializer, command)

  /** The command [bytes] hold. */
  public fun commandFromBinary(bytes: ByteArray): Command =
    binary.decodeFromByteArray(commandSerializer, bytes)

  /**
   * The entities of [world] that [filter] lets through, each with all its components, in entity
   * order, as JSON.
   */
  public fun worldToJson(world: World, filter: EntityFilter = EntityFilter.ALL): String {
    val byId = world.snapshot().entries.sortedBy { it.key.id }
    val entities =
      byId
        .filter { (entity, _) -> filter.ids?.contains(entity.id) ?: true }
        .filter { (_, snapshot) -> filter.component?.let { carries(snapshot, it) } ?: true }
        .map { EntityState(it.key, it.value) }
    return json.encodeToString(worldSerializer, entities)
  }

  private fun carries(snapshot: Snapshot, serialName: String): Boolean =
    snapshot.components.any { component ->
      module.getPolymorphic(Component::class, component)?.descriptor?.serialName == serialName
    }

  private companion object {
    const val TYPE_KEY = "type"
  }
}

/** A codec for the built-in commands and components and those [register] adds. */
public fun worldCodec(register: CommandRegistry.() -> Unit = {}): WorldCodec {
  val registry = CommandRegistry()
  registry.registerBuiltins()
  registry.register()
  return WorldCodec(registry)
}
