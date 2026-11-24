package dev.chieppa.ease.commands

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.Snapshot
import com.github.quillraven.fleks.World
import dev.chieppa.ease.commands.builtin.registerBuiltins
import dev.chieppa.ease.commands.wire.Wire
import dev.chieppa.ease.commands.wire.WireDecoder
import dev.chieppa.ease.commands.wire.WireEncoder
import dev.chieppa.ease.commands.wire.WireTable
import dev.chieppa.ease.ecs.replication.BitWriter
import dev.chieppa.ease.ecs.replication.BitWriterFull
import dev.chieppa.ease.ecs.replication.ReplicatedSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.Json

/**
 * Reads and writes commands and world state: JSON for agents, scripts and plugins, a compact binary
 * form for the command log and the network. Both come from the same `@Serializable` declarations.
 * The binary form names no fields and numbers each registered command and component type by its
 * serial name's place in sorted order, so peers that register the same types read each other.
 */
public class WorldCodec internal constructor(registry: CommandRegistry) {
  private val module = registry.module()
  private val json = Json {
    serializersModule = module
    classDiscriminator = TYPE_KEY
    // Every field is written, defaults included, so a reader never has to know a type's defaults.
    encodeDefaults = true
  }
  private val commandTable = WireTable(Command::class.java, registry.commandTypes())
  private val tables =
    listOf(commandTable, WireTable(Component::class.java, registry.componentTypes()))
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

  /** How many command types are registered; [commandTypeId] runs from 0 to this, exclusive. */
  public val commandTypeCount: Int
    get() = commandTable.size

  /**
   * A hash of every registered command, component and replicated type's serial name, in the order
   * the binary forms number them: peers whose hashes differ would misread each other's bytes.
   */
  public val typesHash: Long = typesHash(registry)

  /**
   * A writer of commands in the binary form into reused buffers, for one sender, held to what a
   * peer's [commandReader] accepts.
   */
  public fun commandWriter(): CommandWriter = commandWriter(Wire.PEER)

  /** A reader of commands in the binary form received from peers, for one receiver. */
  public fun commandReader(): CommandReader = commandReader(Wire.PEER)

  private fun commandWriter(limits: Wire) =
    CommandWriter(WireEncoder(module, tables, limits), limits)

  private fun commandReader(limits: Wire) = CommandReader(WireDecoder(module, tables, limits))

  /** [command]'s type as the binary form numbers it, or -1 when it is not registered. */
  public fun commandTypeId(command: Command): Int = commandTable.idOf(command)

  /** The command type with serial name [serialName] as the binary form numbers it, or -1. */
  public fun commandTypeId(serialName: String): Int =
    (0 until commandTable.size).firstOrNull { commandTable.nameAt(it) == serialName } ?: -1

  /**
   * [command] in the binary form, for the command log and undo history: held to the log's limits,
   * which are far larger than what a peer may send.
   */
  public fun commandToBinary(command: Command): ByteArray {
    val writer = commandWriter(Wire.LOG)
    var capacity = FIRST_BINARY_BYTES
    while (true) {
      val buffer = BitWriter(capacity)
      try {
        writer.write(command, buffer)
        return buffer.buffer.copyOf(buffer.byteLength)
      } catch (full: BitWriterFull) {
        // A command larger than the network allows may still be logged: grow until it fits.
        if (capacity >= Wire.LOG.maxBytes) throw full
        capacity *= 2
      }
    }
  }

  /**
   * The command [bytes] hold, which [commandToBinary] wrote, read under the log's limits; throws
   * when they hold none.
   */
  public fun commandFromBinary(bytes: ByteArray): Command {
    val reader = commandReader(Wire.LOG)
    return reader.read(bytes, 0, bytes.size) ?: throw SerializationException(reader.detail)
  }

  /**
   * The command [bytes] hold, read as hostile input from a peer, as a [CommandReader] reads it.
   * Malformed bytes give a [ReceivedCommand.Refused], never an exception.
   */
  public fun receiveCommand(bytes: ByteArray): ReceivedCommand {
    val reader = commandReader()
    val command = reader.read(bytes, 0, bytes.size)
    return if (command != null) ReceivedCommand.Accepted(command)
    else ReceivedCommand.Refused(checkNotNull(reader.refusal), reader.detail)
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

    /**
     * The buffer a command is first written into for the log; it doubles until the command fits.
     */
    private const val FIRST_BINARY_BYTES = 256

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
