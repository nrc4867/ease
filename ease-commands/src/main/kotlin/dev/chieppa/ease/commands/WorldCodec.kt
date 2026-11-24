package dev.chieppa.ease.commands

import com.github.quillraven.fleks.World
import dev.chieppa.ease.commands.builtin.registerBuiltins
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.cbor.Cbor
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
  }
  private val binary = Cbor { serializersModule = module }
  private val commandSerializer = PolymorphicSerializer(Command::class)
  private val worldSerializer = ListSerializer(EntityState.serializer())

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

  /** Every entity of [world] with all its components, in entity order, as JSON. */
  public fun worldToJson(world: World): String {
    val byId = world.snapshot().entries.sortedBy { it.key.id }
    val entities = byId.map { EntityState(it.key, it.value) }
    return json.encodeToString(worldSerializer, entities)
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
