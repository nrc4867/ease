package dev.chieppa.ease.codegen.read

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier

/** Whether the serialization plugin writes [this] property: a backing field, not transient. */
internal fun KSPropertyDeclaration.isSerialized(): Boolean =
  hasBackingField && annotation(AnnotationNames.TRANSIENT) == null && extensionReceiver == null

/** Whether [this] class is a value class. */
internal fun KSClassDeclaration.isValueClass(): Boolean =
  Modifier.VALUE in modifiers || Modifier.INLINE in modifiers

/**
 * Whether a supertype other than `kotlin.*` and `java.*` ones is an interface or a class others
 * extend: the class then belongs to a polymorphic base, and travels with its serial name under
 * `type` when a value goes through that base.
 */
internal fun KSClassDeclaration.hasPolymorphicBase(): Boolean =
  superTypes
    .map { it.resolve().declaration }
    .filterIsInstance<KSClassDeclaration>()
    .any { base ->
      val name = base.qualifiedName?.asString().orEmpty()
      !name.startsWith("kotlin.") && !name.startsWith("java.")
    }
