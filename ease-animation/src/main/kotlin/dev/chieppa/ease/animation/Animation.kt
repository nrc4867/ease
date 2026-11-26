package dev.chieppa.ease.animation

import com.github.quillraven.fleks.World
import dev.chieppa.ease.animation.runtime.AnimationAssets
import dev.chieppa.ease.animation.runtime.AnimatorSystem
import dev.chieppa.ease.animation.scene.Animator
import dev.chieppa.ease.animation.scene.SkinRef
import dev.chieppa.ease.animation.scene.SocketFollower
import dev.chieppa.ease.ecs.Phase
import dev.chieppa.ease.ecs.PhasedSystems
import dev.chieppa.ease.scene.types.SceneTypes

/** How a host adds animation to its worlds. */
public object Animation {
  /**
   * Adds an [AnimatorSystem] reading [assets] to the world [systems] builds, in the animation
   * phase, and returns it. Every world gets one, host and clients alike, since each works out its
   * own poses; the world needs `SceneNodes` to find bones and `WorldTransforms` to place them.
   */
  public fun addTo(systems: PhasedSystems, assets: AnimationAssets?): AnimatorSystem =
    AnimatorSystem(assets).also { systems.add(Phase.ANIMATION, it) }

  /** Registers the animation component types scene files hold. */
  public fun registerSceneTypes(types: SceneTypes.Builder) {
    types.component(Animator, Animator.serializer())
    types.component(SkinRef, SkinRef.serializer())
    types.component(SocketFollower, SocketFollower.serializer())
  }
}

/**
 * This world's animators, added by [Animation.addTo]. Finding it searches the world's systems, so a
 * system looks it up once, in `onInit` or on first use, rather than every tick.
 */
public val World.animators: AnimatorSystem
  get() = system<AnimatorSystem>()
