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

  override fun onChange(listener: (ContributorKind<*>) -> Unit): Registration =
    delegate.onChange(listener).also { made += it }

  /**
   * Closes every registration made through this, newest first. A registration whose closing throws,
   * as a listener reacting to the change may, does not stop the others; the first failure is thrown
   * at the end with the rest attached as suppressed.
   */
  @Suppress("TooGenericExceptionCaught") // Listener code runs inside each close.
  override fun close() {
    val failures = mutableListOf<RuntimeException>()
    made.asReversed().forEach {
      try {
        it.close()
      } catch (failure: RuntimeException) {
        failures += failure
      }
    }
    made.clear()
    val first = failures.firstOrNull() ?: return
    failures.drop(1).forEach { first.addSuppressed(it) }
    throw first
  }
}
