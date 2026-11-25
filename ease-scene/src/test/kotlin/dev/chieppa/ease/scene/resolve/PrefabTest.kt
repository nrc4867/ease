package dev.chieppa.ease.scene.resolve

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.scene.Scenes
import dev.chieppa.ease.scene.Scenes.document
import dev.chieppa.ease.scene.Scenes.id
import dev.chieppa.ease.scene.Scenes.node
import dev.chieppa.ease.scene.document.Instance
import dev.chieppa.ease.scene.document.OverridePath
import dev.chieppa.ease.scene.document.SceneDocument
import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.scene.node.NodeKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class PrefabTest {
  private val crateGuid = Guid(0, 1)
  private val stackGuid = Guid(0, 2)
  private val sublevelGuid = Guid(0, 3)

  /** A crate: a Box whose beacon points at its Lid. */
  private var crate =
    document(
      node(10, "Box").withField(Scenes.BEACON, "target", JsonPrimitive(id(11).text)),
      node(11, "Lid", parent = 10),
    )

  private val documents = mutableMapOf<Guid, SceneDocument>()
  private val resolver = SceneResolver(Scenes.types, { documents[it] }) { it != Guid(9, 9) }

  private fun lidX(path: Long) = OverridePath(listOf(id(path)), Scenes.TRANSFORM, "x")

  /** A room with two crates, the second with its lid moved to x 5. */
  private fun room(): SceneDocument {
    documents[crateGuid] = crate
    val moved = Instance(crateGuid).withOverride(lidX(11), JsonPrimitive(5))
    return document(
      node(1, "Room"),
      node(2, "Crate A", parent = 1, instance = Instance(crateGuid)),
      node(3, "Crate B", parent = 1, instance = moved),
    )
  }

  private fun key(vararg ids: Long): NodeKey =
    NodeKey.of(ids.dropLast(1).map(::LocalId), LocalId(ids.last()))

  private fun ResolvedScene.x(vararg ids: Long): Float =
    checkNotNull(this[key(*ids)])
      .components
      .getValue(Scenes.TRANSFORM)
      .getValue("x")
      .jsonPrimitive
      .float

  private fun ResolvedScene.parentOf(vararg ids: Long): String? =
    this[key(*ids)]
      ?.components
      ?.get("ease.Parent")
      ?.get("node")
      ?.jsonObject
      ?.get("path")
      ?.jsonPrimitive
      ?.content

  @Test
  fun `each copy of a prefab gets keys of its own under its placing node`() {
    val scene = resolver.resolve(room())

    assertEquals(
      listOf(key(1), key(2), key(2, 10), key(2, 11), key(3), key(3, 10), key(3, 11)),
      scene.nodes.map { it.key },
    )
    assertEquals(key(2).text, scene.parentOf(2, 10))
    assertEquals(key(2, 10).text, scene.parentOf(2, 11))
    assertEquals(key(1).text, scene.parentOf(3))
    assertEquals(emptyList(), scene.problems)
  }

  @Test
  fun `a node slot points at the node of its own copy`() {
    val scene = resolver.resolve(room())

    val target = { ids: LongArray ->
      checkNotNull(scene[key(*ids)]).components.getValue(Scenes.BEACON).getValue("target")
    }
    assertEquals(JsonPrimitive(key(2, 11).text), target(longArrayOf(2, 10)))
    assertEquals(JsonPrimitive(key(3, 11).text), target(longArrayOf(3, 10)))
  }

  @Test
  fun `an override changes its own instance only`() {
    val scene = resolver.resolve(room())

    assertEquals(0f, scene.x(2, 11))
    assertEquals(5f, scene.x(3, 11))
  }

  @Test
  fun `a prefab change reaches every instance and overrides stay on top`() {
    val placed = room()
    crate = crate.with(node(11, "Lid", parent = 10, x = 3f)).with(node(12, "Latch", parent = 10))
    documents[crateGuid] = crate

    val scene = resolver.resolve(placed)

    assertEquals(3f, scene.x(2, 11))
    assertEquals(5f, scene.x(3, 11))
    assertEquals(key(2, 10).text, scene.parentOf(2, 12))
    assertEquals(key(3, 10).text, scene.parentOf(3, 12))
  }

  @Test
  fun `the outermost override wins in a prefab placed inside a prefab`() {
    room()
    documents[stackGuid] =
      document(
        node(
          20,
          "Inner crate",
          instance = Instance(crateGuid).withOverride(lidX(11), JsonPrimitive(2)),
        )
      )
    val outer = OverridePath(listOf(id(20), id(11)), Scenes.TRANSFORM, "x")
    val scene =
      document(
        node(4, "Plain stack", instance = Instance(stackGuid)),
        node(
          5,
          "Moved stack",
          instance = Instance(stackGuid).withOverride(outer, JsonPrimitive(9)),
        ),
      )

    val resolved = resolver.resolve(scene)

    assertEquals(2f, resolved.x(4, 20, 11))
    assertEquals(9f, resolved.x(5, 20, 11))
  }

  @Test
  fun `a sublevel placed twice never collides`() {
    documents[sublevelGuid] = document(node(30, "Torch"), node(31, "Stand", parent = 30))
    val scene =
      document(
        node(6, "East", instance = Instance(sublevelGuid)),
        node(7, "West", instance = Instance(sublevelGuid)),
      )

    val resolved = resolver.resolve(scene)

    assertEquals(6, resolved.nodes.size)
    assertEquals(key(6, 30).text, resolved.parentOf(6, 31))
    assertEquals(key(7, 30).text, resolved.parentOf(7, 31))
  }

  @Test
  fun `what cannot be honored is reported and the rest still resolves`() {
    room()
    documents[stackGuid] = document(node(40, "Loop", instance = Instance(stackGuid)))
    val scene =
      document(
        node(1, "Room"),
        node(8, "Loop holder", instance = Instance(stackGuid)),
        node(9, "Gone", instance = Instance(Guid(0, 77))),
        node(2, "Crate", instance = Instance(crateGuid).withOverride(lidX(99), JsonPrimitive(1))),
        node(3, "Lamp").withField(Scenes.BEACON, "glow", JsonPrimitive(Guid(9, 9).toString())),
        node(4, "Orphan", parent = 55),
        node(5, "Odd").withField("test.Unknown", "a", JsonPrimitive(1)),
      )

    val resolved = resolver.resolve(scene)

    val kinds = resolved.problems.map { it::class.simpleName }.toSet()
    assertEquals(
      setOf("Cycle", "MissingAsset", "StaleOverride", "MissingParent", "BadComponent"),
      kinds,
    )
    assertIs<SceneProblem.Cycle>(resolved.problems.first { it is SceneProblem.Cycle })
    assertNull(resolved.parentOf(4))
    assertTrue(resolved[key(5)] != null)
    assertEquals(0f, resolved.x(2, 11))
  }

  @Test
  fun `an empty asset slot is no problem, and a bad one names its component`() {
    val scene =
      document(
        node(1, "Dark").withField(Scenes.BEACON, "glow", JsonNull),
        node(2, "Odd").withField(Scenes.BEACON, "glow", JsonPrimitive("not a guid")),
      )

    val problems = resolver.resolve(scene).problems

    val bad = problems.map { assertIs<SceneProblem.BadComponent>(it) }
    assertTrue(bad.isNotEmpty())
    assertTrue(bad.all { it.node == key(2) && it.component == Scenes.BEACON }, problems.toString())
    assertTrue(bad.any { "not a guid" in it.reason })
  }

  @Test
  fun `parents come before their children whatever their keys`() {
    val scene = document(node(1, "Lid", parent = 2), node(2, "Jar", parent = 9), node(9, "Shelf"))

    val order = resolver.resolve(scene).nodes.map { it.key }

    assertEquals(listOf(key(9), key(2), key(1)), order)
  }
}
