package dev.chieppa.ease.animation

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.animation.assets.AnimatorAsset
import dev.chieppa.ease.animation.assets.AnimatorBlend
import dev.chieppa.ease.animation.assets.AnimatorBlendPoint
import dev.chieppa.ease.animation.assets.AnimatorCondition
import dev.chieppa.ease.animation.assets.AnimatorParameter
import dev.chieppa.ease.animation.assets.AnimatorProbe
import dev.chieppa.ease.animation.assets.AnimatorSocket
import dev.chieppa.ease.animation.assets.AnimatorState
import dev.chieppa.ease.animation.assets.AnimatorTransition
import dev.chieppa.ease.animation.assets.ChannelPath
import dev.chieppa.ease.animation.assets.ClipAsset
import dev.chieppa.ease.animation.assets.ClipChannel
import dev.chieppa.ease.animation.assets.ClipEvent
import dev.chieppa.ease.animation.assets.Comparison
import dev.chieppa.ease.animation.assets.Interpolation
import dev.chieppa.ease.animation.assets.ParameterDrive
import dev.chieppa.ease.animation.assets.SkinAsset
import dev.chieppa.ease.animation.runtime.AnimationAssets
import dev.chieppa.ease.animation.scene.Animator
import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.node.NodeName
import dev.chieppa.ease.scene.node.OrderKey
import dev.chieppa.ease.scene.node.Parent
import dev.chieppa.ease.scene.slots.AssetRef
import kotlin.math.cos
import kotlin.math.sin

/**
 * A two-legged test model and its assets: an animator node placing hips, two legs and two feet
 * (left at x = -0.2, right at x = +0.2, soles at y = 0 in the rest pose), an idle clip that only
 * moves the hips, a one-second walk clip that swings the legs about x with a footstep event per
 * foot, and an animator asset with an idle state and a move state blending idle into walk by speed.
 */
object TestRig {
  val ANIMATOR: Guid = Guid.parse("6c1a6f0e-1b7e-4f43-9c2a-2f4f8f0a0001")
  val IDLE: Guid = Guid.parse("6c1a6f0e-1b7e-4f43-9c2a-2f4f8f0a0002")
  val WALK: Guid = Guid.parse("6c1a6f0e-1b7e-4f43-9c2a-2f4f8f0a0003")
  val SKIN: Guid = Guid.parse("6c1a6f0e-1b7e-4f43-9c2a-2f4f8f0a0004")

  /** Where the walk clip has full weight in the move state's blend, world units a second. */
  const val WALK_SPEED = 1.5f

  /** When each foot comes down in the walk clip, seconds into it. */
  const val LEFT_DOWN = 0.105f
  const val RIGHT_DOWN = 0.605f

  val HIPS = LocalId(0x10)
  val LEG_LEFT = LocalId(0x11)
  val FOOT_LEFT = LocalId(0x12)
  val LEG_RIGHT = LocalId(0x13)
  val FOOT_RIGHT = LocalId(0x14)

  private const val SWING = 0.4f
  private const val HIP_HEIGHT = 1f
  private const val LEG_LENGTH = 1f
  private const val HIP_WIDTH = 0.2f
  private const val KEYS = 9

  val idle =
    ClipAsset(
      "Idle",
      1f,
      listOf(
        ClipChannel(
          HIPS.text,
          ChannelPath.TRANSLATION,
          Interpolation.LINEAR,
          floatArrayOf(0f, 0.5f, 1f),
          floatArrayOf(0f, HIP_HEIGHT, 0f, 0f, HIP_HEIGHT - 0.05f, 0f, 0f, HIP_HEIGHT, 0f),
        )
      ),
    )

  val walk =
    ClipAsset(
      "Walk",
      1f,
      listOf(swing(LEG_LEFT, 1f), swing(LEG_RIGHT, -1f)),
      listOf(ClipEvent("footstepLeft", LEFT_DOWN), ClipEvent("footstepRight", RIGHT_DOWN)),
    )

  /**
   * A leg swinging forward and back about x once a second, [sign] picking its half of the cycle.
   */
  private fun swing(leg: LocalId, sign: Float): ClipChannel {
    val times = FloatArray(KEYS) { it / (KEYS - 1f) }
    val values =
      FloatArray(KEYS * 4).also { quats ->
        for (key in 0 until KEYS) {
          val half = sign * SWING * sin(times[key] * 2f * Math.PI.toFloat()) / 2f
          quats[key * 4] = sin(half)
          quats[key * 4 + 3] = cos(half)
        }
      }
    return ClipChannel(leg.text, ChannelPath.ROTATION, Interpolation.LINEAR, times, values)
  }

  /** The animator asset, [speed] driven as given. */
  fun animator(drive: ParameterDrive = ParameterDrive.GAME): AnimatorAsset =
    AnimatorAsset(
      parameters = listOf(AnimatorParameter("speed", drive)),
      states =
        listOf(
          AnimatorState("idle", clip = IDLE),
          AnimatorState(
            "move",
            blend =
              AnimatorBlend(
                "speed",
                listOf(AnimatorBlendPoint(IDLE, 0f), AnimatorBlendPoint(WALK, WALK_SPEED)),
              ),
          ),
        ),
      transitions =
        listOf(
          AnimatorTransition(
            "idle",
            "move",
            listOf(AnimatorCondition("speed", Comparison.ABOVE, 0.05f)),
            duration = 0.25f,
          ),
          AnimatorTransition(
            "move",
            "idle",
            listOf(AnimatorCondition("speed", Comparison.BELOW, 0.05f)),
            duration = 0.25f,
          ),
        ),
      sockets =
        listOf(
          AnimatorSocket("soleLeft", "footLeft", y = 0f),
          AnimatorSocket("hand", "hips", x = 0.5f),
        ),
      probes =
        listOf(
          AnimatorProbe("leftFoot", "soleLeft", "footstepLeft"),
          AnimatorProbe("rightFoot", "footRight", "footstepRight"),
        ),
    )

  /** The assets of the rig, the animator's speed driven as given. */
  fun assets(drive: ParameterDrive = ParameterDrive.GAME): AnimationAssets =
    object : AnimationAssets {
      override val changes: Int = 0
      override val problems: Collection<String> = emptyList()
      private val animator = animator(drive)

      override fun animator(guid: Guid): AnimatorAsset? = animator.takeIf { guid == ANIMATOR }

      override fun clip(guid: Guid): ClipAsset? =
        when (guid) {
          IDLE -> idle
          WALK -> walk
          else -> null
        }

      override fun skin(guid: Guid): SkinAsset? =
        SkinAsset(
            listOf(LEG_LEFT.text, LEG_RIGHT.text),
            listOf("legLeft", "legRight"),
            FloatArray(32) { if (it % 16 % 5 == 0) 1f else 0f },
            null,
          )
          .takeIf { guid == SKIN }
    }

  /** The entities of one placed rig. */
  class Placed(val animator: Entity, val footLeft: Entity, val footRight: Entity, val hips: Entity)

  /** Places the rig in [world] under the key [id], its animator node at ([x], 0, [z]). */
  fun place(world: World, id: Long = 1, x: Float = 0f, z: Float = 0f): Placed {
    val key = NodeKey.of(LocalId(id))
    val animator = world.entity {
      it += key
      it += NodeName("rig")
      it +=
        LocalTransform().apply {
          this.x = x
          this.z = z
        }
      it += Animator(AssetRef(ANIMATOR))
    }
    val hips = node(world, key, HIPS, key, "hips") { y = HIP_HEIGHT }
    node(world, key, LEG_LEFT, key.child(HIPS), "legLeft") { this.x = -HIP_WIDTH }
    val footLeft = node(world, key, FOOT_LEFT, key.child(LEG_LEFT), "footLeft") { y = -LEG_LENGTH }
    node(world, key, LEG_RIGHT, key.child(HIPS), "legRight") { this.x = HIP_WIDTH }
    val footRight =
      node(world, key, FOOT_RIGHT, key.child(LEG_RIGHT), "footRight") { y = -LEG_LENGTH }
    return Placed(animator, footLeft, footRight, hips)
  }

  @Suppress("LongParameterList")
  private fun node(
    world: World,
    placement: NodeKey,
    id: LocalId,
    parent: NodeKey,
    name: String,
    place: LocalTransform.() -> Unit,
  ): Entity = world.entity {
    it += placement.child(id)
    it += NodeName(name)
    it += Parent(parent, OrderKey("m"))
    it += LocalTransform().apply(place)
  }
}
