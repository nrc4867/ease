package dev.chieppa.ease.ecs.replication

import kotlin.test.Test
import kotlin.test.assertEquals

/** Nested replicated classes of one name each get their own serializer. */
class NestedSerializerTest {
  @Test
  fun `two nested classes of one name get serializers named by their whole path`() {
    assertEquals("dev.chieppa.ease.ecs.replication.Shelf.Lamp", Shelf_LampReplicated.name)
    assertEquals("dev.chieppa.ease.ecs.replication.Hall.Lamp", Hall_LampReplicated.name)
    assertEquals(listOf("level"), Shelf_LampReplicated.fieldNames)
    assertEquals(listOf("lit"), Hall_LampReplicated.fieldNames)
  }
}
