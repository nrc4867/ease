package dev.chieppa.ease.commands

import com.github.quillraven.fleks.Snapshot
import dev.chieppa.ease.commands.builtin.Destroy
import dev.chieppa.ease.commands.builtin.Restore
import dev.chieppa.ease.commands.builtin.SetTransform
import dev.chieppa.ease.commands.builtin.Spawn
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.replication.NetworkId
import dev.chieppa.ease.math.Quat
import dev.chieppa.ease.math.Transform
import dev.chieppa.ease.math.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.serialization.SerializationException

class WorldCodecTest {
  private val codec = worldCodec { command(Nudge::class, Nudge.serializer()) }
  private val entity = NetworkId(7)
  private val transform =
    Transform(Vec3(1f, -2f, 3.5f), Quat.fromAxisAngle(Vec3(1f, 0f, 0f), 0.25f), Vec3(2f, 2f, 2f))

  private val samples: List<Command> =
    listOf(
      SetTransform(entity, transform),
      Spawn(listOf(LocalTransform.of(transform))),
      Destroy(entity),
      Restore(entity, Snapshot(listOf(LocalTransform.of(transform)), emptyList())),
      Nudge(entity, 0.5f),
    )

  @Test
  fun `every command round-trips through JSON`() {
    samples.forEach { command ->
      val json = codec.commandToJson(command)

      assertEquals(json, codec.commandToJson(codec.commandFromJson(json)))
    }
  }

  @Test
  fun `every command round-trips through the binary form`() {
    samples.forEach { command ->
      val decoded = codec.commandFromBinary(codec.commandToBinary(command))

      assertEquals(codec.commandToJson(command), codec.commandToJson(decoded))
    }
  }

  @Test
  fun `JSON names a command's type`() {
    val json = codec.commandToJson(SetTransform(entity, transform))

    assertTrue("\"type\":\"ease.SetTransform\"" in json, json)
    assertTrue("\"translation\":{\"x\":1.0,\"y\":-2.0,\"z\":3.5}" in json, json)
  }

  @Test
  fun `the binary form is smaller than JSON`() {
    val command = SetTransform(entity, transform)

    assertTrue(codec.commandToBinary(command).size < codec.commandToJson(command).length)
  }

  @Test
  fun `an unregistered command type is refused`() {
    assertFailsWith<SerializationException> {
      worldCodec().commandToJson(Nudge(entity, 1f))
    }
  }
}
