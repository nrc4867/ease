package dev.chieppa.ease.net.view

import com.github.quillraven.fleks.Component
import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.ecs.replication.BitReader
import dev.chieppa.ease.ecs.replication.ReplicatedSerializer
import dev.chieppa.ease.ecs.replication.ReplicatedTypes
import dev.chieppa.ease.ecs.replication.StatePackets
import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.scene.node.NodeKey

/**
 * Reads the parts of a spawn after its kind, network id and owner (see [ViewMessages]), treating
 * every byte as hostile: depths and counts are bounded before anything is read past them, and only
 * registered replicated types are built. The first problem is kept in [refusal]; nothing here
 * touches a world, so a refused spawn changes nothing.
 */
internal class SpawnReader(private val types: ReplicatedTypes, private val reader: BitReader) {
  private val seen = BooleanArray(types.size)

  /** The first problem met, or null. */
  var refusal: ViewRefusal? = null
    private set

  /** Starts a new message. */
  fun reset() {
    refusal = null
  }

  /** Records [problem] unless an earlier one is already recorded. */
  fun refuse(problem: ViewRefusal) {
    if (refusal == null) refusal = problem
  }

  /** Whether neither the bits nor what they say have been refused so far. */
  val ok: Boolean
    get() {
      if (!reader.ok) refuse(ViewRefusal.of(reader.status))
      return refusal == null
    }

  /** Reads the form, or null after refusing it. */
  fun form(): SpawnForm? =
    when (reader.readBits(ViewMessages.FORM_BITS)) {
      ViewMessages.PLAIN -> SpawnForm.Plain
      ViewMessages.SCENE_NODE -> sceneNode()
      else -> null.also { refuse(ViewRefusal.UNKNOWN_FORM) }
    }

  private fun sceneNode(): SpawnForm? {
    val asset = Guid(ViewMessages.readLong(reader), ViewMessages.readLong(reader))
    val placementDepth = reader.readBits(ViewMessages.DEPTH_BITS)
    val depth = reader.readBits(ViewMessages.DEPTH_BITS)
    if (depth !in 1..ViewMessages.MAX_KEY_DEPTH || placementDepth >= depth) {
      refuse(ViewRefusal.BAD_KEY)
    }
    val ids = LongArray(if (ok) depth else 0) { ViewMessages.readLong(reader) }
    return if (ok) SpawnForm.SceneNode(asset, placementDepth, keyOf(ids)) else null
  }

  private fun keyOf(ids: LongArray): NodeKey =
    NodeKey.of(List(ids.size - 1) { LocalId(ids[it]) }, LocalId(ids.last()))

  /**
   * Reads the replicated components: each into the component of its type in [base] when there is
   * one, so fields the spawn leaves out keep the local file's value, or into a new one. Returns
   * them with the components of [base] that do not replicate, or null after a refusal.
   */
  fun components(base: List<Component<out Any>>): List<Component<out Any>>? {
    val count = reader.readBits(StatePackets.COMPONENT_COUNT_BITS)
    if (count > types.size) refuse(ViewRefusal.TOO_MANY_COMPONENTS)
    val result = base.filterTo(mutableListOf()) { types.idOf(it.type()) < 0 }
    seen.fill(false)
    var read = 0
    while (ok && read < count) {
      val typeId = reader.readBits(ReplicatedTypes.ID_BITS)
      when {
        !ok -> Unit
        typeId >= types.size -> refuse(ViewRefusal.UNKNOWN_TYPE)
        seen[typeId] -> refuse(ViewRefusal.DUPLICATE_COMPONENT)
        else -> {
          seen[typeId] = true
          val serializer = types[typeId].serializer
          result += readInto(serializer, base.firstOrNull { it.type() === serializer.type })
        }
      }
      read++
    }
    return if (ok) result else null
  }

  private fun <T : Component<T>> readInto(
    serializer: ReplicatedSerializer<T>,
    base: Component<*>?,
  ): Component<out Any> {
    @Suppress("UNCHECKED_CAST") // Matched to the serializer by its component type.
    val target = base as T? ?: serializer.create()
    serializer.read(reader, target)
    return target
  }
}
