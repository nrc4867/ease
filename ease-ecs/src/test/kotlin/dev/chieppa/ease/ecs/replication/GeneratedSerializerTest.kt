package dev.chieppa.ease.ecs.replication

import kotlin.test.Test
import kotlin.test.assertEquals

class GeneratedSerializerTest {
  @Test
  fun `the processor writes an object beside a replicated class`() {
    assertEquals("dev.chieppa.ease.ecs.replication.Beacon", BeaconReplicated.NAME)
  }
}
