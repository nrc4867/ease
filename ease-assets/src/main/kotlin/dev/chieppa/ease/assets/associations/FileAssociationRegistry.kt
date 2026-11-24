package dev.chieppa.ease.assets.associations

/** The in-memory [FileAssociations]: one registry per project, shared by everything that asks. */
public class FileAssociationRegistry : FileAssociations {
  private val entries = mutableListOf<Entry>()
  private val listeners = mutableListOf<(ContributorKind<*>) -> Unit>()

  override fun <T : Contributor> register(
    kind: ContributorKind<T>,
    match: FileMatch,
    priority: Int,
    contributor: T,
    origin: String,
  ): Registration {
    val entry = Entry(kind, match, priority, contributor, origin)
    entries += entry
    notify(kind)
    return Handle({ entry in entries }) {
      if (entries.remove(entry)) notify(kind)
    }
  }

  override fun <T : Contributor> resolve(kind: ContributorKind<T>, file: ProjectFile): List<T> {
    val ranked = matching(file).filter { it.kind === kind }
    if (kind.cardinality == Cardinality.ONE && ranked.size > 1 && tied(ranked[0], ranked[1])) {
      val contenders = ranked.filter { tied(ranked[0], it) }
      throw AmbiguousContributors(file, kind, contenders.map { it.attachment() })
    }
    return ranked.map { kind.type.javaObjectType.cast(it.contributor) }
  }

  override fun attachments(file: ProjectFile): List<Attachment> =
    matching(file).map { it.attachment() }

  override fun onChange(listener: (ContributorKind<*>) -> Unit): Registration {
    listeners += listener
    return Handle({ listener in listeners }) { listeners.remove(listener) }
  }

  /** The entries that match [file], best first; registration order breaks the rest. */
  private fun matching(file: ProjectFile): List<Entry> =
    entries
      .filter { it.match.matches(file) }
      .sortedWith(
        compareByDescending<Entry> { it.match.specificity }.thenByDescending { it.priority }
      )

  private fun tied(first: Entry, second: Entry): Boolean =
    first.match.specificity == second.match.specificity && first.priority == second.priority

  private fun notify(kind: ContributorKind<*>) {
    listeners.toList().forEach { it(kind) }
  }

  private class Entry(
    val kind: ContributorKind<*>,
    val match: FileMatch,
    val priority: Int,
    val contributor: Contributor,
    val origin: String,
  ) {
    fun attachment() = Attachment(kind, contributor, match, priority, origin)
  }

  private class Handle(private val isOpenNow: () -> Boolean, private val onClose: () -> Unit) :
    Registration {
    override val isOpen: Boolean
      get() = isOpenNow()

    override fun close() {
      if (isOpenNow()) onClose()
    }
  }
}
