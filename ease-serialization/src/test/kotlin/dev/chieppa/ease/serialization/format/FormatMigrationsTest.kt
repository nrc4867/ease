package dev.chieppa.ease.serialization.format

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class FormatMigrationsTest {
  private val migrations =
    FormatMigrations(
      current = 3,
      steps =
        mapOf(
          1 to { file -> JsonObject(file + ("addedInTwo" to JsonPrimitive(true))) },
          2 to { file -> JsonObject(file - "addedInTwo" + ("addedInThree" to JsonPrimitive(1))) },
        ),
    )

  private fun file(version: Int) =
    JsonObject(mapOf(FORMAT_VERSION_KEY to JsonPrimitive(version), "keep" to JsonPrimitive("me")))

  @Test
  fun `an old file runs every step up to the current version`() {
    val result = migrations.migrate(file(1))

    assertEquals(
      JsonObject(
        mapOf(
          FORMAT_VERSION_KEY to JsonPrimitive(3),
          "keep" to JsonPrimitive("me"),
          "addedInThree" to JsonPrimitive(1),
        )
      ),
      result,
    )
  }

  @Test
  fun `a current file is unchanged`() {
    assertEquals(file(3), migrations.migrate(file(3)))
  }

  @Test
  fun `a file from a newer Ease is refused`() {
    assertFailsWith<UnsupportedFormatVersion> { migrations.migrate(file(4)) }
  }

  @Test
  fun `a file without a version is refused`() {
    assertFailsWith<UnsupportedFormatVersion> { migrations.migrate(JsonObject(emptyMap())) }
  }

  @Test
  fun `a gap in the steps is refused`() {
    val gappy = FormatMigrations(current = 3, steps = mapOf(2 to { it }))

    assertFailsWith<UnsupportedFormatVersion> { gappy.migrate(file(1)) }
  }
}
