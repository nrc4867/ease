package dev.chieppa.ease.assets.associations

import kotlin.reflect.KClass

/**
 * A kind of contributor, such as importers or previews: a name for lists and messages, the
 * interface its contributors implement, and whether a file takes one of them or many.
 */
public class ContributorKind<T : Contributor>(
  public val name: String,
  public val type: KClass<T>,
  public val cardinality: Cardinality,
) {
  override fun toString(): String = name
}
