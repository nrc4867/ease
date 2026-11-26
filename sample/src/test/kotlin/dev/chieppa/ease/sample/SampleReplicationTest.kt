package dev.chieppa.ease.sample

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.animation.scene.Animator
import dev.chieppa.ease.animation.scene.SkinRef
import dev.chieppa.ease.ecs.replication.Authority
import dev.chieppa.ease.ecs.replication.BitReader
import dev.chieppa.ease.ecs.replication.BitWriter
import dev.chieppa.ease.ecs.replication.DecodeStatus
import dev.chieppa.ease.ecs.replication.NetworkIdentity
import dev.chieppa.ease.ecs.replication.ReplicatedFields
import dev.chieppa.ease.ecs.replication.ReplicatedSerializer
import dev.chieppa.ease.physics.body.Collider
import dev.chieppa.ease.render.scene.MaterialRef
import dev.chieppa.ease.render.scene.MeshRef
import dev.chieppa.ease.runtime.LocalHost
import dev.chieppa.ease.runtime.loop.FixedStep
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.node.NodeName
import dev.chieppa.ease.scene.node.Parent
import dev.chieppa.ease.scene.types.SceneTypes
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

/**
 * Every component the sample's entities carry replicates, and round-trips through its serializer,
 * but identity and a node's place in its scene, which spawns carry instead.
 */
class SampleReplicationTest {
  @TempDir lateinit var folder: Path

  private val content by lazy { SampleContent(folder) }
  private val local by lazy {
    LocalHost(SampleGame(), FixedStep.perSecond(), CourierScript(), content.assets)
  }
  private val world by lazy { local.host }
  private val writer = BitWriter(CAPACITY)
  private val reader = BitReader()

  /**
   * Sends [entity]'s component through [serializer] and returns the fields that came back wrong.
   */
  private fun <T : Component<T>> wrongFields(
    serializer: ReplicatedSerializer<T>,
    world: World,
    entity: Entity,
  ): Int {
    val sent = serializer.componentOf(world, entity)
    writer.reset()
    serializer.write(writer, sent, ReplicatedFields.allFields(serializer.fieldCount))
    reader.reset(writer.buffer, writer.byteLength)
    val received = serializer.create()
    serializer.read(reader, received)
    assertEquals(DecodeStatus.OK, reader.status, serializer.name)
    return serializer.changedFields(received, sent)
  }

  /** The serial name of the replicated type [component] is, failing when it has none. */
  private fun nameOf(component: Component<*>): String {
    val serializers = world.codec.replicated
    return serializers.single { it.type == component.type() }.name
  }

  /**
   * Whether [component] is a node's key, name or parent, which a scene node's spawn names, or one
   * each peer builds from its own files and never sends: an asset slot, an animator, a skin, a
   * collider, or the stroller's state, which only the host's walk reads.
   */
  private fun isNode(component: Component<*>): Boolean =
    component is NodeKey ||
      component is NodeName ||
      component is Parent ||
      component::class.java in FROM_FILES

  @Test
  fun `the sample registers a serializer for every component its entities carry`() {
    repeat(TICKS) { local.step() }
    val serializers = world.codec.replicated
    val replicated = serializers.map { it.name }.toSet()

    val snapshots = world.world.snapshot().values
    val components = snapshots.flatMap { it.components }
    val carried =
      components
        .filterNot { it is NetworkIdentity || it is Authority || isNode(it) }
        .map { nameOf(it) }
        .toSet()

    val scene = setOf("ease.Camera", "ease.DirectionalLight", "ease.Mesh", "ease.Material")
    assertEquals(scene + "ease.LocalTransform" + "sample.Velocity", carried - COURIER)
    assertTrue(COURIER in carried)
    assertEquals(carried, replicated)
  }

  @Test
  fun `every sample component round-trips, quantized fields to the same step`() {
    repeat(TICKS) { local.step() }
    var checked = 0

    with(world.world) {
      asEntityBag().forEach { entity ->
        world.codec.replicated
          .filter { entity.has(it.type) }
          .forEach { serializer ->
            assertEquals(0, wrongFields(serializer, world.world, entity), serializer.name)
            checked++
          }
      }
    }

    // Every component a scene node holds replicates, but for those each peer builds from its own
    // files; a placed node's parent and name are named by its spawn.
    val nodeTypes =
      content.placed().sumOf { node ->
        node.components.keys.count { it !in ASSET_SLOTS && it !in SceneTypes.STRUCTURAL }
      }
    assertEquals(nodeTypes + COURIER_TYPES, checked)
  }

  private companion object {
    const val CAPACITY = 256
    const val TICKS = 45
    const val COURIER = "sample.Courier"
    const val COURIER_TYPES = 5
    val ASSET_SLOTS =
      setOf(
        "ease.MeshRef",
        "ease.MaterialRef",
        "ease.Animator",
        "ease.SkinRef",
        "ease.Collider",
        "sample.Stroller",
      )

    val FROM_FILES: Set<Class<*>> =
      setOf(
        MeshRef::class.java,
        MaterialRef::class.java,
        Animator::class.java,
        SkinRef::class.java,
        Collider::class.java,
        Stroller::class.java,
      )
  }
}
