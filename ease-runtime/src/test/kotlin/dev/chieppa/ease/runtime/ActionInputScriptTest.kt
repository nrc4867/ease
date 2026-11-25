package dev.chieppa.ease.runtime

import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.replication.Authority
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.net.input.InputLayout
import dev.chieppa.ease.platform.input.ActionDefinition
import dev.chieppa.ease.platform.input.ActionInput
import dev.chieppa.ease.platform.input.ActionKind
import dev.chieppa.ease.platform.input.ActionSet
import dev.chieppa.ease.platform.input.InputContext
import dev.chieppa.ease.platform.input.RecordedInput
import dev.chieppa.ease.runtime.loop.FixedStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Actions driven by a recorded device reach the predicted walker exactly as raw input would. */
class ActionInputScriptTest {
  private val actions =
    ActionSet(
      listOf(ActionDefinition("move", ActionKind.AXIS), ActionDefinition("jump", ActionKind.BUTTON))
    )

  /** Right for 30 ticks, nothing for 10, then left for 20; `key.d` right, `key.a` left. */
  private val recording =
    List(RIGHT) { mapOf("key.d" to 1f) } +
      List(PAUSE) { emptyMap() } +
      List(LEFT) { mapOf("key.a" to 1f) }

  private fun walkerX(local: LocalHost): Float =
    with(local.host.world) {
      family { all(LocalTransform, Authority) }
        .first { it[Authority].owner != PeerId.HOST }[LocalTransform]
        .x
    }

  private fun actionHost(): LocalHost {
    val gameplay = InputContext("gameplay").bind("move", "key.d").bind("move", "key.a", -1f)
    val input = ActionInput(RecordedInput(recording), actions)
    input.contexts.push(gameplay)
    return LocalHost(
      WalkModule(),
      FixedStep.perSecond(),
      ActionInputScript(input, InputLayout(axes = 2, buttons = 8), listOf("move"), listOf("jump")),
    )
  }

  private fun rawHost(): LocalHost {
    var tick = 0
    val raw = InputScript { client ->
      val axis = if (tick < RIGHT) 1f else if (tick < RIGHT + PAUSE) 0f else -1f
      client.inputFrame.setAxis(0, if (tick < recording.size) axis else 0f)
      tick++
    }
    return LocalHost(WalkModule(), FixedStep.perSecond(), raw)
  }

  @Test
  fun `a recorded device moves the predicted walker the same as the raw input it stands for`() {
    val withActions = actionHost()
    val raw = rawHost()

    repeat(STEPS) {
      withActions.step()
      raw.step()
    }

    assertTrue(walkerX(withActions) != 0f, "the walker moved")
    assertEquals(walkerX(raw), walkerX(withActions))
  }

  @Test
  fun `two runs of the same recording end in the same place`() {
    val first = actionHost()
    val second = actionHost()

    repeat(STEPS) {
      first.step()
      second.step()
    }

    assertEquals(walkerX(first), walkerX(second))
  }

  @Test
  fun `more actions than the layout holds are refused`() {
    val input = ActionInput(RecordedInput(recording), actions)

    assertFailsWith<IllegalArgumentException> {
      ActionInputScript(input, InputLayout(axes = 0, buttons = 8), listOf("move"), emptyList())
    }
  }

  private companion object {
    const val RIGHT = 30
    const val PAUSE = 10
    const val LEFT = 20
    const val STEPS = 120
  }
}
