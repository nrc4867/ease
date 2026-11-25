package dev.chieppa.ease.assets.associations

/**
 * The file association service: Ease never hardcodes what a file type means. Contributors register
 * for the files they handle, and whatever needs one asks here.
 */
public interface FileAssociations {
  /**
   * Registers [contributor] for the files [match] applies to. Among matches, the more specific one
   * wins, then the higher [priority]; [origin] says where the contributor came from, for inspectors
   * and error messages. Close the returned handle to remove it.
   */
  public fun <T : Contributor> register(
    kind: ContributorKind<T>,
    match: FileMatch,
    priority: Int,
    contributor: T,
    origin: String = "",
  ): Registration

  /**
   * The contributors of [kind] for [file] that [accepts] takes, best first. The filter runs before
   * ties are looked for, so contributors that differ in what [accepts] asks about never tie. For a
   * kind that takes one contributor, throws [AmbiguousContributors] when the two best tie.
   */
  public fun <T : Contributor> resolve(
    kind: ContributorKind<T>,
    file: ProjectFile,
    accepts: (T) -> Boolean = { true },
  ): List<T>

  /** Every registration of [kind], whatever it matches, in registration order. */
  public fun registered(kind: ContributorKind<*>): List<Attachment>

  /** Every contributor attached to [file], of every kind, with where each came from. */
  public fun attachments(file: ProjectFile): List<Attachment>

  /**
   * Calls [listener] with the kind that changed whenever a contributor of it is registered or
   * removed, so files can re-resolve. Close the handle to stop listening.
   */
  public fun onChange(listener: (ContributorKind<*>) -> Unit): Registration
}
