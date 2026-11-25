package dev.chieppa.ease.scene.runtime

import com.github.quillraven.fleks.World
import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.loading.AssetStore
import dev.chieppa.ease.commands.CommandHistory
import dev.chieppa.ease.commands.CommandOrigin
import dev.chieppa.ease.commands.CommandRejected
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.resolve.SceneProblem

/**
 * Spawns scenes into the world of [history] and keeps them in step with their files, on the peer
 * holding authority. [place] spawns a scene or prefab; at each tick boundary, after the asset
 * reloads, [beforeTick] looks at the files every placed scene was built from (the scene, and every
 * prefab and sublevel in it), and at those it needed but could not load. When one has reloaded,
 * appeared or changed, it resolves the scene again and applies the difference from the live nodes
 * as `SpawnNode`, `UpdateNode` and `DestroyNode` commands with the engine's origin. The commands go
 * through the history, so they are logged; clients receive the nodes as spawns naming the placed
 * asset and each node's key (see [placementOf]). A tick where no file changed allocates nothing.
 */
public class SceneSync(
  world: World,
  private val store: AssetStore,
  private val history: CommandHistory,
) {
  private val nodes = world.system<SceneNodes>()
  private val diff = SceneDiff(world, nodes)
  private val placed = ArrayList<PlacedScene>()
  private val byKey = HashMap<NodeKey, PlacedScene>()

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

  /**
   * The placed scene whose last applied version spawned the node [key], or null when no placed
   * scene has it, such as a node an editor command made: what a spawn names the node by.
   */
  public fun placementOf(key: NodeKey): PlacedScene? = byKey[key]

  /** Applies the changes of every placed scene whose files reloaded since the last call. */
  public fun beforeTick(tick: Long) {
    for (index in placed.indices) {
      val scene = placed[index]
      if (scene.isStale(store.database)) apply(scene, tick)
    }
  }

  private fun apply(scene: PlacedScene, tick: Long) {
    val source = TrackingSource(store)
    val resolved = source.resolver(nodes.types).resolve(scene.asset, scene.placement)
    val problems = resolved.problems.toMutableList()
    for (command in diff.commands(resolved, scene.keys)) {
      try {
        history.execute(command, tick, CommandOrigin.GAME)
      } catch (rejected: CommandRejected) {
        problems += SceneProblem.Rejected(rejected.message.orEmpty())
      }
    }
    scene.keys.forEach { byKey.remove(it, scene) }
    scene.keys = resolved.nodes.map { it.key }
    scene.keys.forEach { byKey[it] = scene }
    scene.problems = problems
    source.trackIn(scene)
  }
}
