package dev.chieppa.ease.assets.associations

/** One contributor attached to a file, as an inspector lists it. */
public class Attachment(
  /** The kind of contributor. */
  public val kind: ContributorKind<*>,
  /** The contributor itself. */
  public val contributor: Contributor,
  /** What it registered for. */
  public val match: FileMatch,
  /** Its priority. */
  public val priority: Int,
  /** Where it came from: an Ease module, a game module or a plugin. */
  public val origin: String,
)
