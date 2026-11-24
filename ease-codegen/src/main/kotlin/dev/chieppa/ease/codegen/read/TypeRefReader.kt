package dev.chieppa.ease.codegen.read

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.Modifier
import dev.chieppa.ease.codegen.model.PrimitiveKind
import dev.chieppa.ease.codegen.model.TypeRef

/**
 * Turns a Kotlin type into a [TypeRef]. A class it meets that has a schema of its own is recorded
 * in [discovered] under its serial name, so the reader of classes can describe it later; this
 * reader never reads one, which keeps types that refer to each other from looping.
 */
internal class TypeRefReader(private val discovered: MutableMap<String, KSClassDeclaration>) {
  /** [type] as a reference. */
  fun read(type: KSType): TypeRef {
    if (type.isMarkedNullable) return TypeRef.Nullable(read(type.makeNotNullable()))
    return when (val declaration = type.declaration) {
      is KSTypeAlias -> read(declaration.type.resolve())
      is KSClassDeclaration -> ofClass(declaration, type)
      else -> TypeRef.Unknown
    }
  }

  /** The reference to a field whose `@Serializable(with = …)` names the serializer [with]. */
  fun readSerializedWith(with: KSType): TypeRef {
    val serializer = with.declaration as? KSClassDeclaration ?: return TypeRef.Unknown
    val target = serializer.annotation(AnnotationNames.SERIALIZED_AS)?.argument("type")
    return if (target is KSType) read(target) else TypeRef.Unknown
  }

  private fun ofClass(declaration: KSClassDeclaration, type: KSType): TypeRef {
    val name = declaration.qualifiedName?.asString()
    val primitive = name?.let { PRIMITIVES[it] }
    return when {
      primitive != null -> TypeRef.Primitive(primitive)
      name in LISTS -> TypeRef.ListOf(argument(type, 0))
      name in MAPS -> TypeRef.MapOf(argument(type, 0), argument(type, 1))
      describesItself(declaration) -> named(declaration)
      isOpenBase(declaration) -> TypeRef.Open(declaration.simpleName.asString())
      else -> TypeRef.Unknown
    }
  }

  private fun argument(type: KSType, index: Int): TypeRef =
    type.arguments.getOrNull(index)?.type?.resolve()?.let { read(it) } ?: TypeRef.Unknown

  private fun named(declaration: KSClassDeclaration): TypeRef {
    val serialName = declaration.serialName()
    discovered.putIfAbsent(serialName, declaration)
    return TypeRef.Named(serialName)
  }

  private fun describesItself(declaration: KSClassDeclaration): Boolean =
    declaration.classKind == ClassKind.ENUM_CLASS ||
      declaration.isSerializable() && !isOpenBase(declaration)

  private fun isOpenBase(declaration: KSClassDeclaration): Boolean =
    Modifier.SEALED !in declaration.modifiers &&
      (declaration.classKind == ClassKind.INTERFACE ||
        Modifier.ABSTRACT in declaration.modifiers ||
        Modifier.OPEN in declaration.modifiers)

  private companion object {
    val PRIMITIVES =
      mapOf(
        "kotlin.Boolean" to PrimitiveKind.BOOLEAN,
        "kotlin.Byte" to PrimitiveKind.INTEGER,
        "kotlin.Short" to PrimitiveKind.INTEGER,
        "kotlin.Int" to PrimitiveKind.INTEGER,
        "kotlin.Long" to PrimitiveKind.INTEGER,
        "kotlin.UByte" to PrimitiveKind.INTEGER,
        "kotlin.UShort" to PrimitiveKind.INTEGER,
        "kotlin.UInt" to PrimitiveKind.INTEGER,
        "kotlin.ULong" to PrimitiveKind.INTEGER,
        "kotlin.Float" to PrimitiveKind.NUMBER,
        "kotlin.Double" to PrimitiveKind.NUMBER,
        "kotlin.Char" to PrimitiveKind.STRING,
        "kotlin.String" to PrimitiveKind.STRING,
      )

    val LISTS =
      setOf(
        "kotlin.collections.List",
        "kotlin.collections.MutableList",
        "kotlin.collections.Set",
        "kotlin.collections.MutableSet",
        "kotlin.collections.Collection",
        "kotlin.Array",
      )

    val MAPS = setOf("kotlin.collections.Map", "kotlin.collections.MutableMap")
  }
}
