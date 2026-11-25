package dev.chieppa.ease.runtime

import dev.chieppa.ease.scene.resolve.SceneProblem
import dev.chieppa.ease.scene.runtime.PlacedScene
import dev.chieppa.ease.scene.runtime.SceneSync
import java.util.IdentityHashMap

/**
 * What the host's placed [scenes] could not honor, such as a node component whose type the game
 * never registered with `registerSceneTypes`. Each time a scene is applied, at its placing and at
 * every reload, its problems are new, and [drain] hands them on once. A tick where no scene was
 * applied allocates nothing.
 */
public class SceneProblemLog internal constructor(private val scenes: SceneSync?) {
  private val reported = IdentityHashMap<PlacedScene, List<SceneProblem>>()
  private val checked = IdentityHashMap<PlacedScene, List<SceneProblem>>()

  /** Every problem of every placed scene as it was last applied. */
  public val problems: List<SceneProblem>
    get() = scenes?.scenes.orEmpty().flatMap { it.problems }

  /** How many [problems] there are. */
  public val count: Int
    get() = scenes?.scenes.orEmpty().sumOf { it.problems.size }

  /** Hands [sink] each problem of every scene applied since the last call. */
  public fun drain(sink: (SceneProblem) -> Unit) {
    sinceLast(reported) { problems -> for (problem in problems) sink(problem) }
  }

  /**
   * Throws, naming each, when a placed scene holds a component of a type the game does not
   * register: a debug build or a headless run stops on it, where a released game keeps running.
   */
  public fun checkTypes() {
    failOnUnknown(problems)
  }

  /** [checkTypes] for the scenes applied since the last call alone, allocating nothing else. */
  internal fun checkApplied() {
    sinceLast(checked, ::failOnUnknown)
  }

  private fun failOnUnknown(problems: List<SceneProblem>) {
    if (problems.none { it is SceneProblem.UnknownComponent }) return
    val unknown = problems.filterIsInstance<SceneProblem.UnknownComponent>()
    error(
      "Scene components with no registered scene type (add them in registerSceneTypes): " +
        unknown.joinToString("; ") { it.message }
    )
  }

  /** Calls [each] with the problems of every scene applied since [seen] last saw it. */
  private inline fun sinceLast(
    seen: IdentityHashMap<PlacedScene, List<SceneProblem>>,
    each: (List<SceneProblem>) -> Unit,
  ) {
    val placed = scenes?.scenes ?: return
    for (index in placed.indices) {
      val scene = placed[index]
      val problems = scene.problems
      if (seen[scene] === problems) continue
      seen[scene] = problems
      each(problems)
    }
  }
}
