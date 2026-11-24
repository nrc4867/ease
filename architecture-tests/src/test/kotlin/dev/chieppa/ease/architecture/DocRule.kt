package dev.chieppa.ease.architecture

import com.lemonappdev.konsist.api.container.KoScope
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.declaration.KoPropertyDeclaration

/**
 * The rule that every public serialized type and field carries `@Doc`, in place of KDoc: the
 * violations in a scope, each as `path: Type` or `path: Type.field`.
 */
object DocRule {
  private const val SERIALIZABLE = "Serializable"
  private const val TRANSIENT = "Transient"
  private const val DOC = "Doc"

  /** What [scope] declares without a `@Doc`: public `@Serializable` classes and their fields. */
  fun violations(scope: KoScope): List<String> =
    scope.classes(includeNested = true).filter(::isPublicSerializable).flatMap { type ->
      val missing = mutableListOf<String>()
      if (!type.hasAnnotationWithName(DOC)) missing += "${type.path}: ${type.name}"
      serializedFields(type)
        .filterNot { it.hasAnnotationWithName(DOC) }
        .mapTo(missing) { "${type.path}: ${type.name}.${it.name}" }
      missing
    }

  private fun isPublicSerializable(type: KoClassDeclaration): Boolean =
    type.hasAnnotationWithName(SERIALIZABLE) &&
      type.hasPublicOrDefaultModifier &&
      enclosedPublicly(type)

  /** Whether every class around [type] is public too, so the type is part of the public API. */
  private fun enclosedPublicly(type: KoClassDeclaration): Boolean =
    generateSequence(type.containingDeclaration as? KoClassDeclaration) {
        it.containingDeclaration as? KoClassDeclaration
      }
      .all { it.hasPublicOrDefaultModifier }

  /** The public properties the serialization plugin writes: with a backing field, not transient. */
  private fun serializedFields(type: KoClassDeclaration): List<KoPropertyDeclaration> =
    type.properties(includeNested = false).filter { property ->
      property.hasPublicOrDefaultModifier &&
        !property.hasAnnotationWithName(TRANSIENT) &&
        !property.hasConstModifier &&
        !property.isExtension &&
        property.delegateName == null &&
        property.getter == null &&
        (property.isConstructorDefined || property.isInitialized)
    }
}
