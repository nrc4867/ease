package dev.chieppa.ease.ecs.replication

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

/** Holds a replicated class whose name another class's nested class shares. */
internal class Shelf {
  /** Public, but nested in an internal class, so only internal to code outside. */
  @Replicated
  class Lamp : Component<Lamp> {
    var level: Float = 0f

    override fun type() = Lamp

    companion object : ComponentType<Lamp>()
  }
}

/** Holds the other nested class named `Lamp`. */
internal class Hall {
  @Replicated
  class Lamp : Component<Lamp> {
    var lit: Boolean = false

    override fun type() = Lamp

    companion object : ComponentType<Lamp>()
  }
}
