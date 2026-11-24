package dev.chieppa.ease.codegen.read

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType

/** The annotation on [this] whose class is [qualifiedName], or null when it has none. */
internal fun KSAnnotated.annotation(qualifiedName: String): KSAnnotation? =
  annotations.firstOrNull {
    it.annotationType.resolve().qualifiedName() == qualifiedName
  }

/** Whether [this] declaration carries `@Serializable`. */
internal fun KSAnnotated.isSerializable(): Boolean =
  annotation(AnnotationNames.SERIALIZABLE) != null

/**
 * The serializer [this] declaration names with `@Serializable(with = X::class)`, or null when it
 * has none: a bare `@Serializable` leaves `with` at its default, `KSerializer`.
 */
internal fun KSAnnotated.customSerializer(): KSType? {
  val with = annotation(AnnotationNames.SERIALIZABLE)?.argument("with") as? KSType ?: return null
  return with.takeUnless { it.qualifiedName() == AnnotationNames.DEFAULT_SERIALIZER }
}

/** The qualified name of the type's declaration, or null for one without a name. */
internal fun KSType.qualifiedName(): String? = declaration.qualifiedName?.asString()

/** The value of the argument named [name]. */
internal fun KSAnnotation.argument(name: String): Any? =
  arguments.firstOrNull { it.name?.asString() == name }?.value

/** The name of the enum entry the argument named [name] holds, however KSP hands it over. */
internal fun KSAnnotation.enumArgument(name: String): String? =
  when (val value = argument(name)) {
    is KSClassDeclaration -> value.simpleName.asString()
    is KSType -> value.declaration.simpleName.asString()
    else -> value?.toString()?.substringAfterLast('.')
  }

/** Names of the annotations the readers know, as `ease-ecs` and `ease-annotations` declare them. */
internal object AnnotationNames {
  private const val REPLICATION = "dev.chieppa.ease.ecs.replication"
  const val REPLICATED = "$REPLICATION.Replicated"
  const val QUANTIZED = "$REPLICATION.Quantized"
  const val INTERPOLATED = "$REPLICATION.Interpolated"
  const val REPLICATED_TO = "$REPLICATION.ReplicatedTo"
  const val SERIAL_NAME = "kotlinx.serialization.SerialName"
  const val SERIALIZABLE = "kotlinx.serialization.Serializable"
  const val TRANSIENT = "kotlinx.serialization.Transient"
  const val DEFAULT_SERIALIZER = "kotlinx.serialization.KSerializer"

  private const val ANNOTATIONS = "dev.chieppa.ease.annotations"
  const val DOC = "$ANNOTATIONS.Doc"
  const val RANGE = "$ANNOTATIONS.Range"
  const val SERIALIZED_AS = "$ANNOTATIONS.SerializedAs"
}
