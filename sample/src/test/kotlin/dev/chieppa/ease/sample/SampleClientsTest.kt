package dev.chieppa.ease.sample

import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.replication.Authority
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.runtime.HostedWorld
import dev.chieppa.ease.runtime.LocalHost
import dev.chieppa.ease.runtime.loop.FixedStep
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.io.TempDir

/**
 * Two clients in one process, each with the courier script: each peer gets a courier of its own,
 * and each client's input moves only the courier it owns, so both trace the loop.
 */
class SampleClientsTest {
  @TempDir lateinit var folder: Path

  private val local by lazy {
    val assets = SampleContent(folder).assets
    LocalHost(SampleGame(), FixedStep.perSecond(), CourierScript(), assets).also {
      it.addClient(SECOND, CourierScript())
    }
  }

  private fun courierOf(world: HostedWorld, owner: PeerId): Pair<Float, Float> =
    with(world.world) {
      val courier = family { all(Courier, Authority) }.first { it[Authority].owner == owner }
      courier[LocalTransform].x to courier[LocalTransform].y
    }

  @Test
  fun `each client steers its own courier around the loop`() {
    val corners = listOf(2f to 0f, 2f to 2f, 0f to 2f, 0f to 0f)

    corners.forEach { (x, y) ->
      repeat(CourierScript.TURN_EVERY.toInt()) { local.step() }
      for (owner in listOf(LocalHost.LOCAL_PEER, SECOND)) {
        val (atX, atY) = courierOf(local.host, owner)
        assertEquals(x, atX, TOLERANCE, "$owner's courier x at tick ${local.tick}")
        assertEquals(y, atY, TOLERANCE, "$owner's courier y at tick ${local.tick}")
      }
    }
  }

  private companion object {
    val SECOND = PeerId(2)
    const val TOLERANCE = 1e-3f
  }
}
