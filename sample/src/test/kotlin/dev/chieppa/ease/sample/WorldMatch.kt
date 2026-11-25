package dev.chieppa.ease.sample

import com.github.quillraven.fleks.Component
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.replication.ReplicatedSerializer
import dev.chieppa.ease.runtime.ClientWorld
import kotlin.math.abs
import kotlin.math.floor

/**
 * What a client showed at one moment: the tick its own entities were predicted to ([predicted], the
 * last tick it ran), the host tick it drew the others at ([drawnAt], its interpolation time), the
 * newest snapshot it applied ([snapshot]) and its replicated entities.
 */
internal class ClientView(
  val peer: Long,
  val predicted: Long,
  val drawnAt: Double,
  val snapshot: Long,
  val entities: Map<Long, EntityState>,
)

/**
 * The scene gate's match between a client's world and its host's. The client holds exactly the
 * host's replicated entities as of the newest snapshot it applied (the sample has no relevancy
 * filters, so every entity is relevant to every peer), each under the same network id, holder,
 * owner and node key, with the same replicated component types. Every replicated field then equals
 * the host's, compared on the wire, after quantization:
 * - on the entity the client owns, which it predicts, the host's at the client's own last tick, the
 *   prediction being the present;
 * - on every other entity, a field that interpolates equals the host's blended to the client's
 *   interpolation time t, between host ticks floor(t) and floor(t) + 1, within [POSITION_TOLERANCE]
 *   for a position (float rounding); every other field equals the host's at that snapshot.
 */
internal class WorldMatch(
  private val serializers: List<ReplicatedSerializer<*>>,
  private val host: (Long) -> Map<Long, EntityState>,
) {
  private val byName = serializers.associateBy { it.name }

  /** [client]'s view now, its entities copied. */
  fun view(client: ClientWorld): ClientView {
    val world = client.world
    return ClientView(
      world.peer.value.toLong(),
      client.tick - 1,
      client.interpolationTime,
      client.session.latestSnapshotTick,
      ReplicatedState(serializers).of(world.world),
    )
  }

  /** Every way [view] differs from the host's world; empty when it matches. */
  fun mismatches(view: ClientView): List<String> {
    val atSnapshot = host(view.snapshot)
    val problems = mutableListOf<String>()
    if (atSnapshot.keys != view.entities.keys) {
      problems += "entities ${view.entities.keys.sorted()}, host ${atSnapshot.keys.sorted()}"
    }
    for ((id, shown) in view.entities) {
      val held = atSnapshot[id] ?: continue
      problems += identity(shown, held)
      problems += fields(view, shown, held)
    }
    return problems
  }

  private fun identity(shown: EntityState, held: EntityState): List<String> =
    listOfNotNull(
      "#${shown.id} holder".takeIf { shown.holder != held.holder },
      "#${shown.id} owner".takeIf { shown.owner != held.owner },
      "#${shown.id} key ${shown.key}, host ${held.key}".takeIf { shown.key != held.key },
      "#${shown.id} types ${shown.components.keys}"
        .takeIf {
          shown.components.keys != held.components.keys
        },
    )

  private fun fields(view: ClientView, shown: EntityState, held: EntityState): List<String> {
    if (shown.owner == view.peer) {
      val predicted = checkNotNull(host(view.predicted)[shown.id]) { "#${shown.id} not on host" }
      return shown.components.mapNotNull { (name, value) ->
        val mask = changed(byName.getValue(name), predicted.components.getValue(name), value)
        "#${shown.id} $name fields $mask at tick ${view.predicted}".takeIf { mask != 0 }
      }
    }
    val from = floor(view.drawnAt).toLong()
    val before = host(from)[shown.id]
    val after = host(from + 1)[shown.id]
    return shown.components.mapNotNull { (name, value) ->
      val serializer = byName.getValue(name)
      val drawn = serializer.interpolatedFields != 0
      val expected =
        if (drawn && before != null && after != null) {
          blend(serializer, before.components, after.components, (view.drawnAt - from).toFloat())
        } else {
          held.components.getValue(name)
        }
      mismatch(serializer, expected, value)?.let { "#${shown.id} $name $it" }
    }
  }

  /** Why [value] is not [expected], or null when it is, positions within the tolerance. */
  private fun mismatch(
    serializer: ReplicatedSerializer<*>,
    expected: Component<*>,
    value: Component<*>,
  ): String? {
    var mask = changed(serializer, expected, value)
    if (expected is LocalTransform && value is LocalTransform) {
      val close =
        abs(expected.x - value.x) <= POSITION_TOLERANCE &&
          abs(expected.y - value.y) <= POSITION_TOLERANCE &&
          abs(expected.z - value.z) <= POSITION_TOLERANCE
      if (close) mask = mask and positionFields(serializer).inv()
      if (!close) return "at (${value.x}, ${value.y}), host (${expected.x}, ${expected.y})"
    }
    return if (mask == 0) null else "fields $mask"
  }

  private fun positionFields(serializer: ReplicatedSerializer<*>): Int =
    listOf("x", "y", "z").fold(0) { mask, field ->
      mask or (1 shl serializer.fieldNames.indexOf(field))
    }

  @Suppress("UNCHECKED_CAST") // Both components are of the serializer's type, found by its name.
  private fun <T : Component<T>> changed(
    serializer: ReplicatedSerializer<T>,
    expected: Component<*>,
    value: Component<*>,
  ): Int = serializer.changedFields(value as T, expected as T)

  @Suppress("UNCHECKED_CAST") // Both states hold this serializer's type under its name.
  private fun <T : Component<T>> blend(
    serializer: ReplicatedSerializer<T>,
    from: Map<String, Component<*>>,
    to: Map<String, Component<*>>,
    amount: Float,
  ): T {
    val into = serializer.create()
    serializer.interpolate(
      from.getValue(serializer.name) as T,
      to.getValue(serializer.name) as T,
      amount,
      into,
    )
    return into
  }

  private companion object {
    /** How far apart two blends of the same states may be, in world units: float rounding. */
    const val POSITION_TOLERANCE = 1e-4f
  }
}
