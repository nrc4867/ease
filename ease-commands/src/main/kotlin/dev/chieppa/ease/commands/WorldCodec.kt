package dev.chieppa.ease.commands

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.Snapshot
import com.github.quillraven.fleks.World
import dev.chieppa.ease.commands.builtin.registerBuiltins
import dev.chieppa.ease.ecs.replication.ReplicatedSerializer
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

  /** The serializer of every replicated component type, built-ins first. */
  public val replicated: List<ReplicatedSerializer<*>> = registry.replicatedSerializers()

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
   * The command [bytes] hold, read as hostile input from a peer: bytes past [MAX_COMMAND_BYTES] are
   * refused unread, and only a registered command type can be named, so the bytes never choose what
   * gets built. Malformed bytes give a [ReceivedCommand.Refused], never an exception.
   */
  public fun receiveCommand(bytes: ByteArray): ReceivedCommand {
    if (bytes.size > MAX_COMMAND_BYTES) {
      return ReceivedCommand.Refused(CommandRefusal.TOO_LONG, "${bytes.size} bytes")
    }
    // The CBOR decoder throws more than SerializationException on malformed input (index errors
    // among them, which the fuzz test finds), and no exception may pass the connection boundary.
    @Suppress("TooGenericExceptionCaught")
    return try {
      ReceivedCommand.Accepted(binary.decodeFromByteArray(commandSerializer, bytes))
    } catch (malformed: RuntimeException) {
      ReceivedCommand.Refused(CommandRefusal.MALFORMED, malformed.toString())
    }
  }

  /**
   * The entities of [world] that [filter] lets through, each under its network id with all its
   * other components, in network id order, as JSON.
   */
  public fun worldToJson(world: World, filter: EntityFilter = EntityFilter.ALL): String {
    val entities =
      world
        .snapshot()
        .values
        .map { snapshot -> EntityState.of(snapshot) }
        .sortedBy { it.entity.value }
        .filter { state -> filter.ids?.contains(state.entity) ?: true }
        .filter { state -> filter.component?.let { carries(state.snapshot, it) } ?: true }
    return json.encodeToString(worldSerializer, entities)
  }

  private fun carries(snapshot: Snapshot, serialName: String): Boolean =
    snapshot.components.any { component ->
      module.getPolymorphic(Component::class, component)?.descriptor?.serialName == serialName
    }

  public companion object {
    /** The longest command a peer may send, in the binary form. */
    public const val MAX_COMMAND_BYTES: Int = 16 * 1024

    private const val TYPE_KEY = "type"
  }
}

/** A codec for the built-in commands and components and those [register] adds. */
public fun worldCodec(register: CommandRegistry.() -> Unit = {}): WorldCodec {
  val registry = CommandRegistry()
  registry.registerBuiltins()
  registry.register()
  return WorldCodec(registry)
}
