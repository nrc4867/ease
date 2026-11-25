package dev.chieppa.ease.scene.runtime

import com.github.quillraven.fleks.World
import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.loading.AssetHandle
import dev.chieppa.ease.assets.loading.AssetLoadException
import dev.chieppa.ease.assets.loading.AssetStore
import dev.chieppa.ease.commands.CommandHistory
import dev.chieppa.ease.commands.CommandOrigin
import dev.chieppa.ease.commands.CommandRejected
import dev.chieppa.ease.scene.document.SceneDocument
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.resolve.SceneProblem
import dev.chieppa.ease.scene.resolve.SceneResolver
import dev.chieppa.ease.scene.resolve.SceneSource

/**
 * Spawns scenes into the world of [history] and keeps them in step with their files, on the peer
 * holding authority. [place] spawns a scene or prefab; at each tick boundary, after the asset
 * reloads, [beforeTick] looks at the files every placed scene was built from (the scene, and every
 * prefab and sublevel in it), and at those it needed but could not load. When one has reloaded,
 * appeared or changed, it resolves the scene again and applies the difference from the live nodes
 * as `SpawnNode`, `UpdateNode` and `DestroyNode` commands with the engine's origin. The commands go
 * through the history, so they are logged and reach every peer the same way. A tick where no file
 * changed allocates nothing.
 */
public class SceneSync(
  world: World,
  private val store: AssetStore,
  private val history: CommandHistory,
) {
  private val nodes = world.system<SceneNodes>()
  private val diff = SceneDiff(world, nodes)
  private val placed = ArrayList<PlacedScene>()

  /** The scenes placed so far, oldest first. */
  public val scenes: List<PlacedScene>
    get() = placed

  /**
   * Spawns the scene or prefab [asset] under [placement] (null for the outermost scene) at [tick]
   * and keeps it in step with its files from then on.
   */
  public fun place(asset: Guid, tick: Long, placement: NodeKey? = null): PlacedScene {
    val scene = PlacedScene(asset, placement)
    placed += scene
    apply(scene, tick)
    return scene
  }

  /** Applies the changes of every placed scene whose files reloaded since the last call. */
  public fun beforeTick(tick: Long) {
    for (index in placed.indices) {
      val scene = placed[index]
      if (scene.isStale(store.database)) apply(scene, tick)
    }
  }

  private fun apply(scene: PlacedScene, tick: Long) {
    val handles = mutableListOf<AssetHandle<SceneDocument>>()
    val failed = LinkedHashMap<Guid, String?>()
    val source = SceneSource { guid ->
      val handle = handleOf(guid)
      if (handle == null) failed[guid] = store.database[guid]?.contentHash else handles += handle
      handle?.value
    }
    val resolver = SceneResolver(nodes.types, source) { store.database[it] != null }
    val resolved = resolver.resolve(scene.asset, scene.placement)
    val problems = resolved.problems.toMutableList()
    for (command in diff.commands(resolved, scene.keys)) {
      try {
        history.execute(command, tick, CommandOrigin.GAME)
      } catch (rejected: CommandRejected) {
        problems += SceneProblem.Rejected(rejected.message.orEmpty())
      }
    }
    scene.keys = resolved.nodes.map { it.key }
    scene.problems = problems
    scene.track(handles, failed)
  }

  @Suppress("SwallowedException") // A missing or broken file is reported as a scene problem.
  private fun handleOf(guid: Guid): AssetHandle<SceneDocument>? =
    try {
      store.handle(guid, SceneDocument::class)
    } catch (missing: AssetLoadException) {
      null
    }
}
