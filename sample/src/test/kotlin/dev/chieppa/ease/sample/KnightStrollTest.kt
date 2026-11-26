package dev.chieppa.ease.sample

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.animation.animators
import dev.chieppa.ease.animation.events.AnimationListener
import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.runtime.LocalHost
import dev.chieppa.ease.runtime.loop.FixedStep
import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.runtime.SceneNodes
import dev.chieppa.ease.scene.transform.WORLD_TRANSFORM_FLOATS
import dev.chieppa.ease.scene.transform.WorldTransforms
import java.nio.file.Path
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

/**
 * The sample's imported knight strolls across stone and grass: it idles while it waits, walks at
 * walking speed and blends the two by speed as it speeds up; each foot's probe, run on that foot's
 * footstep event, finds the ground under that foot; and a client works out the same walk from the
 * replicated transform alone, its bones never sent.
 */
class KnightStrollTest {
  @TempDir lateinit var folder: Path

  private fun knightOf(world: World): Entity =
    checkNotNull(world.systemOrNull<SceneNodes>()).index[KNIGHT]

  @Test
  fun `the knight idles when still, walks at walking speed and blends between by speed`() {
    val sample = SampleWorld(folder)
    val world = sample.hosted.world
    val knight = knightOf(world)
    val animators = world.animators
    val blended = mutableListOf<Pair<Float, Float>>()

    sample.step(HALF_SECOND)
    assertEquals("idle", animators.stateOf(knight))
    assertEquals(1f, animators.clipWeight(knight, IDLE))

    repeat(RAMP_TICKS) {
      sample.step(1)
      val walk = animators.clipWeight(knight, WALK)
      if (animators.stateOf(knight) == "move" && walk > 0f && walk < 1f) {
        blended += walk to animators.parameter(knight, "speed")
      }
    }
    assertTrue(blended.size > MIN_BLENDED, "too few blended ticks: $blended")
    val settled = blended.drop(FADE_TICKS)
    for ((walk, speed) in settled) assertEquals(speed / WALK_SPEED, walk, TOLERANCE)

    sample.step(CRUISE_TICKS)
    assertEquals("move", animators.stateOf(knight))
    assertEquals(1f, animators.clipWeight(knight, WALK), TOLERANCE)
    assertEquals(WALK_SPEED, animators.parameter(knight, "speed"), TOLERANCE)
  }

  @Test
  fun `each foot's probe, on its footstep, reports the ground under that foot`() {
    val sample = SampleWorld(folder)
    val world = sample.hosted.world
    val knight = knightOf(world)
    val reports = mutableListOf<Report>()
    var footsteps = 0
    world.animators.events.add(
      object : AnimationListener {
        override fun clipEvent(animator: Entity, event: String) {
          if (animator == knight && event.startsWith("footstep")) footsteps++
        }

        override fun probe(
          animator: Entity,
          probe: String,
          surface: Int,
          x: Float,
          y: Float,
          z: Float,
        ) {
          if (animator == knight) reports += Report(probe, surface, x)
        }
      }
    )

    sample.step(ACROSS_TICKS)

    assertEquals(footsteps, reports.size)
    assertEquals(setOf("leftFoot", "rightFoot"), reports.map { it.probe }.toSet())
    val names = reports.filter { abs(it.x) > SEAM }.map { SampleGame.SURFACES[it.surface].name }
    assertEquals(setOf("stone", "grass"), names.toSet())
    for (report in reports.filter { abs(it.x) > SEAM }) {
      val expected = if (report.x < 0f) "stone" else "grass"
      assertEquals(expected, SampleGame.SURFACES[report.surface].name, "$report")
    }
  }

  @Test
  fun `a client works out the walk from the knight's replicated transform, its bones unsent`() {
    val content = SampleContent(folder)
    val local = LocalHost(SampleGame(), FixedStep.perSecond(), assets = content.assets)

    repeat(WALKING_TICKS) { local.step() }

    val client = local.client.world.world
    val knight = knightOf(client)
    assertEquals("move", client.animators.stateOf(knight))
    assertEquals(1f, client.animators.clipWeight(knight, WALK), TOLERANCE)
    val hips = checkNotNull(client.systemOrNull<SceneNodes>()).index[KNIGHT.child(HIPS)]
    val rest = with(client) { hips[LocalTransform] }
    val placed = FloatArray(WORLD_TRANSFORM_FLOATS)
    client.system<WorldTransforms>().write(hips, placed, 0)
    assertEquals(HIPS_REST_Y, rest.y, 1e-6f)
    assertTrue(abs(placed[1] - (KNIGHT_Y + HIPS_REST_Y)) > 1e-4f, "the hips are not posed")
  }

  private data class Report(val probe: String, val surface: Int, val x: Float)

  private companion object {
    val MODEL: Guid = Guid.parse("f9cd5f95-5c21-4573-95f6-f8d27b2065a3")
    val IDLE: Guid = MODEL.named("Idle.clip")
    val WALK: Guid = MODEL.named("Walking_A.clip")
    val KNIGHT: NodeKey = NodeKey.of(LocalId.parse("486b54129f64428b"))

    /** The hips' node in the knight's prefab: glTF node 23 of the trimmed file. */
    val HIPS: LocalId = LocalId.parse("a0bce8ab51407a42")
    const val HIPS_REST_Y = 0.4056634f
    const val KNIGHT_Y = -4.5f

    const val WALK_SPEED = 0.8f
    const val HALF_SECOND = 30
    const val RAMP_TICKS = 60
    const val FADE_TICKS = 15
    const val MIN_BLENDED = 20
    const val CRUISE_TICKS = 60
    const val ACROSS_TICKS = 540
    const val WALKING_TICKS = 240
    const val SEAM = 0.1f
    const val TOLERANCE = 1e-3f
  }
}
