package dev.chieppa.ease.sample

import com.github.quillraven.fleks.World
import dev.chieppa.ease.assets.reload.ReloadAsset
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.replication.networkIdOf
import dev.chieppa.ease.scene.commands.DestroyNode
import dev.chieppa.ease.scene.commands.UpdateNode
import dev.chieppa.ease.scene.document.SceneDocument
import dev.chieppa.ease.scene.document.SceneNode
import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.node.NodeName
import dev.chieppa.ease.testing.AllocationBudget
import java.nio.file.Path
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.io.TempDir

/**
 * The Phase 2b gate: the sample runs as host plus client from its saved scene, over a link with
 * latency, jitter, loss and duplication ([GateRun.BAD]). The scene's squares spawn on both worlds,
 * the courier moves by the client's predicted input and its pace by a client command, the client
 * world matches the host's (see [WorldMatch]), a scene edited on disk reloads into both through the
 * asset watcher, and the host's log replays to the same host world.
 */
class SceneGateTest {
  @TempDir lateinit var folder: Path

  private val run by lazy { GateRun(folder) }

  @Test
  fun `the saved scene's squares spawn on host and client, the client's built from its files`() {
    run.step(SPAWN_TICKS)

    val scene = run.content.playfield()
    val keys = SampleContent.keysOf(scene)
    assertEquals(keys, run.keys(run.local.host.world))
    assertEquals(keys, run.keys(run.client.world.world))
    val client = run.client.world.world
    scene.nodes.values.forEach { node ->
      val replica = run.node(client, NodeKey.of(emptyList(), node.id))
      assertEquals(node.name, with(client) { replica[NodeName].name }, "named from its file")
    }
    assertEquals(scene.nodes.size + 1, client.numEntities, "the squares and the courier")
  }

  @Test
  fun `the courier moves at once by the client's predicted input, as the host then moves it`() {
    run.step(SPAWN_TICKS)
    val courier = run.courier(run.client.world.world)
    val clientWorld = run.client.world.world
    val id = clientWorld.networkIdOf(courier).value
    val predictions = mutableListOf<Triple<Long, Float, Float>>()

    repeat(PREDICTED_TICKS) {
      run.step()
      val tick = run.client.tick - 1
      assertTrue(tick >= run.local.tick, "the client ran tick $tick before the host")
      val at = with(run.client.world.world) { courier[LocalTransform] }
      predictions += Triple(tick, at.x, at.y)
    }
    run.step(LEAD_TICKS)

    val matched = predictions.count { (tick, x, y) -> hostAt(tick, id).let { near(it, x, y) } }
    val moved =
      predictions.zipWithNext().count { (a, b) -> a.second != b.second || a.third != b.third }
    assertEquals(predictions.size, matched, "every prediction is where the host put the courier")
    assertTrue(moved > PREDICTED_TICKS / 2, "the courier moved on $moved ticks")
  }

  @Test
  fun `a client command sets the courier's pace on host and client`() {
    run.step(SPAWN_TICKS)
    val client = run.client.world.world
    val courier = run.courier(client)
    val host = run.local.host.world
    fun pace(world: World) = with(world) { run.courier(world)[Courier].pace }

    run.client.send(SetPace(client.networkIdOf(courier), FAST))
    val set = run.stepUntil(CONVERGE_TICKS) { pace(host) == FAST && pace(client) == FAST }
    run.step(LEAD_TICKS)

    assertTrue(set, "host pace ${pace(host)}, client pace ${pace(client)}")
    val speed = with(host) { run.courier(host)[Velocity].let { abs(it.x) + abs(it.y) } }
    assertEquals(FAST, speed, TOLERANCE, "the host moves the courier at the new pace")
  }

  @Test
  fun `once the link settles the client world matches the host's`() {
    run.step(RUN_TICKS)
    run.settle()
    run.step(SETTLE_TICKS)

    assertEquals(emptyList(), run.mismatchesNow())
  }

  @Test
  fun `a scene edited on disk mid-run reloads into host and client`() {
    run.step(RUN_TICKS)
    val edited = edit(run.content.playfield())
    val keys = SampleContent.keysOf(edited)

    run.content.edit(edited)
    val reached =
      run.stepUntil(CONVERGE_TICKS) {
        run.keys(run.local.host.world) == keys && run.keys(run.client.world.world) == keys
      }

    assertTrue(reached, "client nodes ${run.keys(run.client.world.world)}, wanted $keys")
    val host = run.local.host
    val log = host.history.log
    val reload = log.all.single { log.commandOf(it) is ReloadAsset }.tick
    val seconds = run.fixedStep.stepNanos / NANOS_PER_SECOND
    edited.nodes.values.forEach { node ->
      val id =
        with(run.local.host.world) {
          networkIdOf(run.node(this, NodeKey.of(emptyList(), node.id))).value
        }
      val x =
        field(node, "ease.LocalTransform", "x") + field(node, "sample.Velocity", "x") * seconds
      assertTrue(near(hostAt(reload, id), x, null), "${node.name} starts again from its file")
    }
    run.settle()
    run.step(SETTLE_TICKS)
    assertEquals(emptyList(), run.mismatchesNow())
  }

  @Test
  fun `replaying the host's log onto a fresh world gives the same host world, reload included`() {
    run.step(SPAWN_TICKS)
    val client = run.client.world.world
    run.client.send(SetPace(client.networkIdOf(run.courier(client)), FAST))
    run.step(SPAWN_TICKS)
    run.content.edit(edit(run.content.playfield()))
    run.step(RUN_TICKS)

    val live = run.local.host
    val replayed = run.replay()

    val log = live.history.log
    val logged = log.all.map { log.commandOf(it) }
    assertTrue(logged.any { it is ReloadAsset } && logged.any { it is SetPace }, "reload and pace")
    assertTrue(logged.any { it is UpdateNode } && logged.any { it is DestroyNode }, "node changes")
    assertEquals(live.codec.worldToJson(live.world), replayed.codec.worldToJson(replayed.world))
  }

  @Test
  fun `a tick of host and client over the bad link allocates nothing`() {
    AllocationBudget.assertWithin(0, { run.local.step() }, WARMUP_TICKS, MEASURED_TICKS)
  }

  /** [document] with the coral square moved, an ivory square added and the moss square gone. */
  private fun edit(document: SceneDocument): SceneDocument {
    val coral = document.nodes.values.single { it.name == "Coral square" }
    val moss = document.nodes.values.single { it.name == "Moss square" }
    val moved = coral.withField("ease.LocalTransform", "x", JsonPrimitive(MOVED_X))
    val ivory =
      SceneNode(IVORY, "Ivory square", null, document.nextOrder(null), coral.components)
        .withField("ease.LocalTransform", "y", JsonPrimitive(IVORY_Y))
        .withField("ease.Quad", "blue", JsonPrimitive(1f))
    return document.with(moved).with(ivory).without(moss.id)
  }

  /** The host's courier or square [id] after tick [tick], as recorded. */
  private fun hostAt(tick: Long, id: Long): LocalTransform {
    val entity = checkNotNull(run.hostAt(tick)[id]) { "#$id is not on the host at tick $tick" }
    return entity.components.getValue("ease.LocalTransform") as LocalTransform
  }

  private fun near(at: LocalTransform, x: Float, y: Float?): Boolean =
    abs(at.x - x) <= TOLERANCE && (y == null || abs(at.y - y) <= TOLERANCE)

  private fun field(node: SceneNode, component: String, name: String): Float =
    SampleContent.field(node, component, name)

  private companion object {
    const val SPAWN_TICKS = 120
    const val PREDICTED_TICKS = 120
    const val LEAD_TICKS = 30
    const val RUN_TICKS = 300
    const val SETTLE_TICKS = 60
    const val CONVERGE_TICKS = 120
    const val WARMUP_TICKS = 5_000
    const val MEASURED_TICKS = 1_000
    const val FAST = 8f
    const val MOVED_X = -6f
    const val IVORY_Y = -1f
    const val TOLERANCE = 1e-3f
    const val NANOS_PER_SECOND = 1_000_000_000f
    val IVORY = LocalId(0x1e0d_5eed_0000_0001)
  }
}
