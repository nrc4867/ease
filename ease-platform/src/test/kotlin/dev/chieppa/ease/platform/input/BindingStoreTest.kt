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

  private val actions =
    ActionSet(
      listOf(
        ActionDefinition("move_x", ActionKind.AXIS),
        ActionDefinition("jump", ActionKind.BUTTON),
        ActionDefinition("confirm", ActionKind.BUTTON),
      )
    )

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
    assertTrue(BindingStore(file).load(listOf(fresh), actions))

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

    assertFalse(BindingStore(file).load(listOf(context), actions))
    assertEquals(gameplay().bindings, context.bindings)
  }

  @Test
  fun `a context the file lacks keeps its defaults`() {
    BindingStore(file).save(listOf(gameplay()))
    val menu = InputContext("menu").bind("confirm", "key.enter")

    BindingStore(file).load(listOf(menu), actions)

    assertEquals(listOf(Binding("confirm", "key.enter")), menu.bindings)
  }

  @Test
  fun `a file with an unknown control is refused`() {
    file.parent.let(Files::createDirectories)
    file.writeText(
      "contexts:\n  gameplay:\n    - action: jump\n      control: mouse.left\nversion: 1\n"
    )

    assertFailsWith<IllegalArgumentException> {
      BindingStore(file).load(listOf(gameplay()), actions)
    }
  }

  @Test
  fun `a file with an unknown action is refused and changes nothing`() {
    val saved = InputContext("gameplay").bind("fly", "key.f")
    BindingStore(file).save(listOf(saved))
    val context = gameplay()

    assertFailsWith<IllegalArgumentException> { BindingStore(file).load(listOf(context), actions) }
    assertEquals(gameplay().bindings, context.bindings)
  }

  @Test
  fun `a bad later context leaves the earlier one unchanged`() {
    val first = InputContext("a").bind("jump", "key.q")
    val second = InputContext("b").bind("fly", "key.q")
    BindingStore(file).save(listOf(first, second))
    val untouched = InputContext("a").bind("jump", "key.space")

    assertFailsWith<IllegalArgumentException> {
      BindingStore(file).load(listOf(untouched, InputContext("b")), actions)
    }
    assertEquals(listOf(Binding("jump", "key.space")), untouched.bindings)
  }

  @Test
  fun `a control no device reads is refused`() {
    BindingStore(file).save(listOf(InputContext("gameplay").bind("jump", "key.nowhere")))

    assertFailsWith<IllegalArgumentException> {
      BindingStore(file).load(listOf(gameplay()), actions)
    }
  }

  @Test
  fun `two contexts with one name are refused on save`() {
    assertFailsWith<IllegalArgumentException> {
      BindingStore(file).save(listOf(gameplay(), gameplay()))
    }
  }

  @Test
  fun `saving over a file replaces it whole`() {
    BindingStore(file).save(listOf(gameplay()))
    BindingStore(file).save(listOf(InputContext("gameplay").bind("jump", "key.q")))

    val fresh = InputContext("gameplay")
    BindingStore(file).load(listOf(fresh), actions)

    assertEquals(listOf(Binding("jump", "key.q")), fresh.bindings)
  }

  @Test
  fun `a blank or relative configuration folder falls back to the home default`() {
    val home = Path.of("home").toAbsolutePath()

    assertEquals(home.resolve(".config/g"), UserDirectory.of("g", "Linux", { "" }, home))
    assertEquals(home.resolve(".config/g"), UserDirectory.of("g", "Linux", { "rel" }, home))
    assertEquals(
      home.resolve("AppData/Roaming/g"),
      UserDirectory.of("g", "Windows 11", { "  " }, home),
    )
  }

  @Test
  fun `the user directory follows the operating system`() {
    val home = Path.of("home").toAbsolutePath()
    val appData = home.resolve("appdata")
    val xdg = home.resolve("xdg")

    assertEquals(
      appData.resolve("g"),
      UserDirectory.of("g", "Windows 11", { if (it == "APPDATA") "$appData" else null }, home),
    )
    assertEquals(
      home.resolve("Library/Application Support/g"),
      UserDirectory.of("g", "Mac OS X", { null }, home),
    )
    assertEquals(home.resolve(".config/g"), UserDirectory.of("g", "Linux", { null }, home))
    assertEquals(xdg.resolve("g"), UserDirectory.of("g", "Linux", { "$xdg" }, home))
  }
}
