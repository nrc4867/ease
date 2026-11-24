package dev.chieppa.ease.sample

/** The box the sample's squares bounce in, centered on the origin, in world units. */
public object Playfield {
  /** How far a square's center goes from the origin along x before it turns back. */
  public const val HALF_WIDTH: Float = 7.5f

  /** How far a square's center goes from the origin along y before it turns back. */
  public const val HALF_HEIGHT: Float = 4f

  /** The height the sample's camera shows, a little more than the box. */
  public const val VISIBLE_HEIGHT: Float = 10f
}
