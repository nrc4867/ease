package dev.chieppa.ease.assets.associations

/**
 * A [FileAssociations] that remembers every registration made through it, so unloading a game
 * module or plugin is one [close]: each contributor it registered is removed and the files they
 * matched re-resolve. Everything else is the [delegate]'s.
 */
public class ModuleRegistrations(private val delegate: FileAssociations) :
  FileAssociations by delegate, Registration {
  private val made = mutableListOf<Registration>()

  override val isOpen: Boolean
    get() = made.any { it.isOpen }

  override fun <T : Contributor> register(
    kind: ContributorKind<T>,
    match: FileMatch,
    priority: Int,
    contributor: T,
    origin: String,
  ): Registration =
    delegate.register(kind, match, priority, contributor, origin).also { made += it }

  /** Closes every registration made through this, newest first. */
  override fun close() {
    made.asReversed().forEach { it.close() }
    made.clear()
  }
}
