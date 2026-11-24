package dev.chieppa.ease.commands

import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.phasedWorld
import dev.chieppa.ease.ecs.replication.networkIdOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/** The types a codec knows, and worlds written through a filter. */
class WorldCodecTypesTest {
  private val codec = worldCodec { command(Nudge::class, Nudge.serializer()) }

  @Test
  fun `the codec lists built-in and registered command types with their fields`() {
    val byName = codec.commandDescriptors.associateBy { it.serialName }

    assertTrue("ease.SetTransform" in byName, byName.keys.toString())
    assertTrue("ease.Spawn" in byName, byName.keys.toString())
    val nudge = byName.getValue("test.Nudge")
    assertEquals(
      listOf("entity", "distance"),
      List(nudge.elementsCount) { nudge.getElementName(it) },
    )
  }

  @Test
  fun `the codec lists the built-in components and the replicated ones`() {
    val names = codec.componentDescriptors.map { it.serialName }
    val replicated = codec.replicated.map { it.name }

    assertEquals(listOf("ease.LocalTransform", "ease.Authority"), names)
    assertEquals(listOf("ease.LocalTransform"), replicated)
  }

  @Test
  fun `a filter keeps the entities named by network id`() {
    val world = phasedWorld {}
    world.entity { it += LocalTransform() }
    val second = world.entity { it += LocalTransform().apply { x = 2f } }

    val id = world.networkIdOf(second)
    val written = Json.parseToJsonElement(codec.worldToJson(world, EntityFilter(setOf(id))))

    val only = written.jsonArray.single()
    assertEquals(id.value.toString(), only.jsonObject.getValue("entity").toString())
    assertTrue("2.0" in written.toString(), written.toString())
  }

  @Test
  fun `a filter keeps the entities carrying a component`() {
    val world = phasedWorld {}
    world.entity {}
    world.entity { it += LocalTransform() }

    val carrying = codec.worldToJson(world, EntityFilter(component = "ease.LocalTransform"))
    val none = codec.worldToJson(world, EntityFilter(component = "test.Missing"))

    assertEquals(1, Json.parseToJsonElement(carrying).jsonArray.size)
    assertEquals(0, Json.parseToJsonElement(none).jsonArray.size)
  }
}
