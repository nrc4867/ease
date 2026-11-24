package dev.chieppa.ease.commands

import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.phasedWorld
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
  fun `the codec lists the built-in components`() {
    val names = codec.componentDescriptors.map { it.serialName }

    assertEquals(listOf("ease.LocalTransform"), names)
  }

  @Test
  fun `a filter keeps the entities named by id`() {
    val world = phasedWorld {}
    world.entity { it += LocalTransform() }
    val second = world.entity { it += LocalTransform().apply { x = 2f } }

    val written = Json.parseToJsonElement(codec.worldToJson(world, EntityFilter(setOf(second.id))))

    val only = written.jsonArray.single()
    val entity = only.jsonObject.getValue("entity")
    assertTrue(entity.toString().contains(second.id.toString()), written.toString())
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
