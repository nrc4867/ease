package dev.chieppa.ease.physics.setup

/**
 * The collision layers a game names (`Player`, `Npc`, `Projectile`) and which pairs of them
 * collide. Every pair collides unless [Builder.ignore] separates it, so a player-only wall is a
 * layer the player collides with and the NPCs ignore. A name nothing declared resolves to the first
 * layer.
 */
public class CollisionLayers private constructor(names: List<String>, ignored: Set<Long>) {
  private val names: List<String> = names.toList()
  private val ignored: Set<Long> = ignored.toSet()
  private val indices: Map<String, Int> =
    names.withIndex().associate { (index, name) -> name to index }

  /** How many layers there are. */
  public val size: Int
    get() = names.size

  /** The name of layer [index]. */
  public fun name(index: Int): String = names[index]

  /** The index of the layer named [name], or -1 when none is. */
  public fun indexOf(name: String): Int = indices[name] ?: -1

  /** The index of the layer named [name]; the first layer for an empty or unknown name. */
  public fun resolve(name: String): Int = indexOf(name).coerceAtLeast(0)

  /** Whether layers [a] and [b] collide. */
  public fun collide(a: Int, b: Int): Boolean = pair(a, b) !in ignored

  /** Names the layers, first to last, and the pairs that do not collide. */
  public class Builder internal constructor() {
    private val names = mutableListOf<String>()
    private val ignored = mutableSetOf<Long>()

    /** Adds the layer [name], after those already added. */
    public fun layer(name: String) {
      require(name.isNotEmpty()) { "A collision layer needs a name" }
      require(name !in names) { "Collision layer $name is named twice" }
      names += name
    }

    /** Makes layers [a] and [b], both already added, pass through each other. */
    public fun ignore(a: String, b: String) {
      ignored += pair(indexOf(a), indexOf(b))
    }

    private fun indexOf(name: String): Int =
      names.indexOf(name).also { require(it >= 0) { "No collision layer is named $name" } }

    internal fun build(): CollisionLayers {
      if (names.isEmpty()) names += DEFAULT
      return CollisionLayers(names, ignored)
    }
  }

  public companion object {
    /** The one layer of a game that names none. */
    public const val DEFAULT: String = "Default"

    /** Builds the layers [block] names; with none named, one [DEFAULT] layer. */
    public fun build(block: Builder.() -> Unit = {}): CollisionLayers =
      Builder().apply(block).build()

    private fun pair(a: Int, b: Int): Long =
      (minOf(a, b).toLong() shl Int.SIZE_BITS) or maxOf(a, b).toLong()
  }
}
