package dev.chieppa.ease.scene.resolve

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.scene.document.Instance
import dev.chieppa.ease.scene.document.OverridePath
import dev.chieppa.ease.scene.document.SceneDocument
import dev.chieppa.ease.scene.document.SceneNode
import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.node.NodeName
import dev.chieppa.ease.scene.node.Parent
import dev.chieppa.ease.scene.types.SceneTypes
import java.util.SortedMap
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Flattens a scene for spawning. Each node's key is its placement's key followed by its local id,
 * so the nodes of every prefab instance and sublevel get keys of their own. A placement's asset
 * comes from [source] and its top nodes hang from the placing node. Overrides are laid over the
 * placed nodes, an enclosing instance's after the inner one's, so the outermost wins. Node slots
 * resolve against the file they are written in, asset slots are checked with [assetExists], and
 * component fields come out in the form their type writes them.
 */
public class SceneResolver(
  private val types: SceneTypes,
  private val source: SceneSource,
  private val assetExists: (Guid) -> Boolean = { true },
) {
  /** The scene or prefab [asset], placed under [placement] (null for the outermost scene). */
  public fun resolve(asset: Guid, placement: NodeKey? = null): ResolvedScene {
    val document = source.document(asset)
    if (document == null) {
      val problem = SceneProblem.MissingAsset(placement ?: ROOT_KEY, asset)
      return ResolvedScene(emptyList(), listOf(asset), listOf(problem))
    }
    return Run().apply { walk(document, placement, emptyList(), listOf(asset)) }.result(asset)
  }

  /** [document], placed under [placement] (null for the outermost scene). */
  public fun resolve(document: SceneDocument, placement: NodeKey? = null): ResolvedScene =
    Run().apply { walk(document, placement, emptyList(), emptyList()) }.result(null)

  /** An override on its way to its node: [path] from the current file, [value] resolved. */
  private class Pending(val path: OverridePath, val value: JsonElement, val instance: NodeKey)

  /** An instance [node] of the file placed at [placement], with its [instance]. */
  private class Placing(val node: SceneNode, val instance: Instance, val placement: NodeKey?)

  private inner class Run {
    val nodes = LinkedHashMap<NodeKey, SortedMap<String, JsonObject>>()
    val parents = HashMap<NodeKey, NodeKey?>()
    val problems = mutableListOf<SceneProblem>()
    val assets = LinkedHashSet<Guid>()

    fun result(asset: Guid?): ResolvedScene {
      val ordered = LinkedHashSet<Guid>().apply { asset?.let(::add) } + assets
      val resolved = TreeOrder.of(parents).map { key -> canonical(key, nodes.getValue(key)) }
      return ResolvedScene(resolved, ordered.toList(), problems.toList())
    }

    fun walk(
      document: SceneDocument,
      placement: NodeKey?,
      overrides: List<Pending>,
      stack: List<Guid>,
    ) {
      document.nodes.values.forEach { add(document, it, placement) }
      document.nodes.values.forEach { node ->
        node.instance?.let { place(Placing(node, it, placement), overrides, stack) }
      }
      overrides.forEach { override ->
        when {
          override.path.nodes.size == 1 -> apply(override, placement)
          document[override.path.nodes[0]]?.instance == null ->
            problems += SceneProblem.StaleOverride(override.instance, override.path.text)
        }
      }
    }

    private fun add(document: SceneDocument, node: SceneNode, placement: NodeKey?) {
      val key = keyOf(placement, node)
      val components = sortedMapOf<String, JsonObject>()
      node.components.forEach { (name, fields) ->
        if (name in SceneTypes.STRUCTURAL) {
          problems += SceneProblem.BadComponent(key, name, "it is set by the node's own fields")
        } else {
          val descriptor = types[name]?.serializer?.descriptor
          components[name] =
            if (descriptor == null) fields
            else rewriterFor(key, placement, name).fields(descriptor, fields)
        }
      }
      components[NAME] = encode(NodeName(node.name))
      val parent = parentOf(document, node, key, placement)
      parent?.let { components[PARENT] = encode(Parent(it, node.order)) }
      parents[key] = parent
      nodes[key] = components
    }

    private fun parentOf(
      document: SceneDocument,
      node: SceneNode,
      key: NodeKey,
      placement: NodeKey?,
    ): NodeKey? {
      val parent = node.parent
      return when {
        parent == null -> placement
        document[parent] != null -> keyOf(placement, parent)
        else -> {
          problems += SceneProblem.MissingParent(key)
          placement
        }
      }
    }

    private fun place(placing: Placing, outer: List<Pending>, stack: List<Guid>) {
      val asset = placing.instance.asset
      val key = keyOf(placing.placement, placing.node)
      val inner = if (asset in stack) null else source.document(asset)
      when {
        asset in stack -> problems += SceneProblem.Cycle(key, asset)
        inner == null -> problems += SceneProblem.MissingAsset(key, asset)
        else -> {
          assets += asset
          val own =
            placing.instance.overrides.map { (path, value) ->
              val rewriter = rewriterFor(key, placing.placement, path.component)
              Pending(path, overrideValue(path, value, rewriter), key)
            }
          val passed =
            outer
              .filter { it.path.nodes.size > 1 && it.path.nodes[0] == placing.node.id }
              .map { Pending(checkNotNull(it.path.inner()), it.value, it.instance) }
          walk(inner, key, own + passed, stack + asset)
        }
      }
    }

    private fun apply(override: Pending, placement: NodeKey?) {
      val path = override.path
      val target = nodes[keyOf(placement, path.nodes[0])]
      val fields = target?.get(path.component)
      val descriptor = types[path.component]?.serializer?.descriptor
      if (fields == null || descriptor != null && descriptor.getElementIndex(path.field) < 0) {
        problems += SceneProblem.StaleOverride(override.instance, path.text)
        return
      }
      target[path.component] = JsonObject(fields + (path.field to override.value))
    }

    private fun overrideValue(
      path: OverridePath,
      value: JsonElement,
      rewriter: SlotRewriter,
    ): JsonElement {
      val descriptor = types[path.component]?.serializer?.descriptor ?: return value
      val index = descriptor.getElementIndex(path.field)
      return if (index < 0) value
      else rewriter.rewrite(descriptor.getElementDescriptor(index), value)
    }

    /** Resolves the slots of [component] on [key], written in the file placed at [placement]. */
    private fun rewriterFor(key: NodeKey, placement: NodeKey?, component: String): SlotRewriter =
      SlotRewriter(placement?.text.orEmpty()) { text ->
        val guid = runCatching { Guid.parse(text) }.getOrNull()
        when {
          guid == null ->
            problems += SceneProblem.BadComponent(key, component, "\"$text\" is not an asset GUID")
          !assetExists(guid) -> problems += SceneProblem.MissingAsset(key, guid)
        }
      }

    /**
     * The node [key] with [components] decoded and written again by their types; those that do not
     * load are reported and named as failed, so a live node keeps its last good value of them.
     */
    private fun canonical(key: NodeKey, components: Map<String, JsonObject>): ResolvedNode {
      val result = sortedMapOf<String, JsonObject>()
      val failed = sortedSetOf<String>()
      components.forEach { (name, fields) ->
        val type = types[name]
        if (type == null) {
          problems += SceneProblem.BadComponent(key, name, "no component type has this name")
          failed += name
        } else {
          runCatching { type.encode(type.decode(fields)) }
            .onSuccess { result[name] = it }
            .onFailure {
              problems += SceneProblem.BadComponent(key, name, it.message.orEmpty())
              failed += name
            }
        }
      }
      return ResolvedNode(key, result, failed)
    }
  }

  private fun keyOf(placement: NodeKey?, node: SceneNode): NodeKey = keyOf(placement, node.id)

  private fun keyOf(placement: NodeKey?, id: LocalId): NodeKey =
    placement?.child(id) ?: NodeKey.of(id)

  private fun encode(name: NodeName): JsonObject = checkNotNull(types[NAME]).encode(name)

  private fun encode(parent: Parent): JsonObject = checkNotNull(types[PARENT]).encode(parent)

  private companion object {
    val NAME: String = NodeName.serializer().descriptor.serialName
    val PARENT: String = Parent.serializer().descriptor.serialName
    val ROOT_KEY: NodeKey = NodeKey.of(LocalId(0))
  }
}
