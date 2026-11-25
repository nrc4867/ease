package dev.chieppa.ease.platform.input

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BindingStoreTest {
  private val directory: Path = Files.createTempDirectory("ease-bindings")
  private val file = UserDirectory.bindingsFile("game", "ana", directory.resolve("game"))

  private fun gameplay() =
    InputContext("gameplay")
      .bind("move_x", "key.d")
      .bind("move_x", "key.a", -1f)
      .bind("jump", "key.space")

  @Test
  fun `rebinding survives a save and load`() {
    val saved = gameplay().also { it.rebind("jump", "key.w") }
    BindingStore(file).save(listOf(saved))

    val fresh = gameplay()
    assertTrue(BindingStore(file).load(listOf(fresh)))

    assertEquals(saved.bindings, fresh.bindings)
  }

  @Test
  fun `the file is canonical YAML`() {
    BindingStore(file).save(listOf(InputContext("gameplay").bind("jump", "key.space")))

    assertEquals(
      "contexts:\n  gameplay:\n    -\n      action: jump\n      control: key.space\n" +
        "      scale: 1.0\nversion: 1\n",
      file.readText(),
    )
  }

  @Test
  fun `a missing file leaves the defaults`() {
    val context = gameplay()

    assertFalse(BindingStore(file).load(listOf(context)))
    assertEquals(gameplay().bindings, context.bindings)
  }

  @Test
  fun `a context the file lacks keeps its defaults`() {
    BindingStore(file).save(listOf(gameplay()))
    val menu = InputContext("menu").bind("confirm", "key.enter")

    BindingStore(file).load(listOf(menu))

    assertEquals(listOf(Binding("confirm", "key.enter")), menu.bindings)
  }

  @Test
  fun `a file with an unknown control is refused`() {
    file.parent.let(Files::createDirectories)
    file.writeText(
      "contexts:\n  gameplay:\n    - action: jump\n      control: mouse.left\nversion: 1\n"
    )

    assertFailsWith<IllegalArgumentException> { BindingStore(file).load(listOf(gameplay())) }
  }

  @Test
  fun `the user directory follows the operating system`() {
    val home = Path.of("home")

    assertEquals(
      Path.of("appdata", "g"),
      UserDirectory.of("g", "Windows 11", { if (it == "APPDATA") "appdata" else null }, home),
    )
    assertEquals(
      home.resolve("Library/Application Support/g"),
      UserDirectory.of("g", "Mac OS X", { null }, home),
    )
    assertEquals(home.resolve(".config/g"), UserDirectory.of("g", "Linux", { null }, home))
    assertEquals(Path.of("xdg", "g"), UserDirectory.of("g", "Linux", { "xdg" }, home))
  }
}
