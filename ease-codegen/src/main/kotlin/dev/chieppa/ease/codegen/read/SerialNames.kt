package dev.chieppa.ease.codegen.read

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

/** The serial name of [this] class: its `@SerialName`, or its qualified name without one. */
internal fun KSClassDeclaration.serialName(): String =
  serialNameAnnotation() ?: qualifiedName?.asString() ?: simpleName.asString()

/** The value of [this] declaration's `@SerialName`, or null when it has none. */
internal fun KSAnnotated.serialNameAnnotation(): String? =
  annotation(AnnotationNames.SERIAL_NAME)?.argument("value") as? String
