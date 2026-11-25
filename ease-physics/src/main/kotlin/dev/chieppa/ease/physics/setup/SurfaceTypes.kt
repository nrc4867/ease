package dev.chieppa.ease.physics.setup

/**
 * The surface types a game declares, by index. Index 0 is the default surface, which colliders
 * naming no surface, or one nothing declared, have.
 */
public class SurfaceTypes private constructor(types: List<SurfaceType>) {
  private val types: List<SurfaceType> = types.toList()
  private val indices: Map<String, Int> =
    types.withIndex().associate { (index, type) -> type.name to index }

  /** How many surface types there are, the default included. */
  public val size: Int
    get() = types.size

  /** The surface type at [index]. */
  public operator fun get(index: Int): SurfaceType = types[index]

  /** The index of the surface type named [name], or -1 when none is. */
  public fun indexOf(name: String): Int = indices[name] ?: -1

  /**
   * The index of the surface type named [name], or the default's for an empty name. A name no
   * surface type has is refused with [IllegalArgumentException].
   */
  public fun resolve(name: String): Int {
    if (name.isEmpty()) return DEFAULT_INDEX
    val index = indexOf(name)
    require(index >= 0) { "No surface type is named \"$name\"; they are ${indices.keys}" }
    return index
  }

  public companion object {
    /** The index of the default surface. */
    public const val DEFAULT_INDEX: Int = 0

    /** The default surface's name. */
    public const val DEFAULT: String = "default"

    /** [types] after a default surface, or after [default] when given. */
    public fun of(
      vararg types: SurfaceType,
      default: SurfaceType = SurfaceType(DEFAULT),
    ): SurfaceTypes {
      val all = listOf(default) + types
      val names = all.map { it.name }
      require(names.toSet().size == names.size) { "A surface type is named twice in $names" }
      return SurfaceTypes(all)
    }
  }
}
