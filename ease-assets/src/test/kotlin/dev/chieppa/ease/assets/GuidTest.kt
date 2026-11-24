package dev.chieppa.ease.assets

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class GuidTest {
  @Test
  fun `a GUID round trips through its text`() {
    val guid = Guid.random()

    assertEquals(guid, Guid.parse(guid.toString()))
    assertEquals(guid.hashCode(), Guid.parse(guid.toString()).hashCode())
  }

  @Test
  fun `random GUIDs are version 4 and differ`() {
    val first = Guid.random()

    assertEquals('4', first.toString()[VERSION_INDEX])
    assertNotEquals(first, Guid.random())
  }

  @Test
  fun `a sub-asset GUID is the version 5 UUID of the parent and the name`() {
    val dnsNamespace = Guid.parse("6ba7b810-9dad-11d1-80b4-00c04fd430c8")

    assertEquals(
      Guid.parse("2ed6657d-e927-568b-95e1-2665a8aea6a2"),
      dnsNamespace.named("www.example.com"),
    )
  }

  @Test
  fun `the same parent and name give the same sub-asset, a different name another`() {
    val parent = Guid.random()

    assertEquals(parent.named("walk"), parent.named("walk"))
    assertNotEquals(parent.named("walk"), parent.named("run"))
    assertTrue(parent.named("walk").toString()[VERSION_INDEX] == '5')
  }

  private companion object {
    const val VERSION_INDEX = 14
  }
}
