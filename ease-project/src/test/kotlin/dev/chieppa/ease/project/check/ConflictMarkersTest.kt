package dev.chieppa.ease.project.check

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConflictMarkersTest {
  private val conflicted = "a\n<<<<<<< ours\nb\n=======\nc\n>>>>>>> theirs\nd\n"

  @Test
  fun `each side settles every block as that side`() {
    assertEquals("a\nb\nd", ConflictMarkers.ours(conflicted))
    assertEquals("a\nc\nd", ConflictMarkers.theirs(conflicted))
  }

  @Test
  fun `a base section is dropped`() {
    val diff3 = "<<<<<<< ours\nb\n||||||| base\nx\n=======\nc\n>>>>>>> theirs\n"

    assertEquals("b", ConflictMarkers.ours(diff3))
    assertEquals("c", ConflictMarkers.theirs(diff3))
  }

  @Test
  fun `text without a complete block is not conflicted`() {
    assertTrue(ConflictMarkers.isConflicted(conflicted))
    assertFalse(ConflictMarkers.isConflicted("a\n=======\nb\n"))
    assertNull(ConflictMarkers.ours("<<<<<<< ours\nb\n=======\nc\n"))
  }
}
