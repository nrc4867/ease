package dev.chieppa.ease.codegen.read

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import dev.chieppa.ease.codegen.model.DescribedType
import dev.chieppa.ease.codegen.model.SerialField
import dev.chieppa.ease.codegen.model.SerialType
import dev.chieppa.ease.codegen.model.TypeKind
import dev.chieppa.ease.codegen.model.TypeRef
import dev.chieppa.ease.codegen.model.references

/**
 * Reads `@Serializable` classes into [SerialType]s: their serial names, kinds, docs, ranges and one
 * [SerialField] for every property the serialization plugin writes, in declaration order. A class a
 * field refers to is read the same way, wherever it is declared, so [describe] gives a type with
 * everything reachable from it.
 */
internal class SerialTypeReader {
  private val discovered = mutableMapOf<String, KSClassDeclaration>()
  private val types = TypeRefReader(discovered)
  private val read = mutableMapOf<String, SerialType>()

  /** [declaration] with every type reachable from it. */
  fun describe(declaration: KSClassDeclaration): DescribedType {
    val root = readOnce(declaration)
    val reachable = linkedMapOf(root.serialName to root)
    val pending = ArrayDeque(root.references())
    while (pending.isNotEmpty()) {
      val name = pending.removeFirst()
      if (name in reachable) continue
      val type = readOnce(checkNotNull(discovered[name]))
      reachable[name] = type
      pending.addAll(type.references())
    }
    return DescribedType(root, reachable)
  }

  private fun readOnce(declaration: KSClassDeclaration): SerialType =
    read.getOrPut(declaration.serialName()) { readClass(declaration) }

  private fun readClass(declaration: KSClassDeclaration): SerialType {
    val serialName = declaration.serialName()
    val doc = declaration.doc()
    return when {
      declaration.classKind == ClassKind.ENUM_CLASS -> enumOf(declaration, serialName, doc)
      Modifier.SEALED in declaration.modifiers -> sealedOf(declaration, serialName, doc)
      else ->
        SerialType(
          serialName,
          if (isValueClass(declaration)) TypeKind.VALUE else TypeKind.CLASS,
          doc,
          fields = fieldsOf(declaration),
          discriminated = hasPolymorphicBase(declaration),
        )
    }
  }

  private fun enumOf(declaration: KSClassDeclaration, serialName: String, doc: String?) =
    SerialType(
      serialName,
      TypeKind.ENUM,
      doc,
      values =
        declaration.declarations
          .filterIsInstance<KSClassDeclaration>()
          .filter { it.classKind == ClassKind.ENUM_ENTRY }
          .map { it.serialNameAnnotation() ?: it.simpleName.asString() }
          .toList(),
    )

  private fun sealedOf(declaration: KSClassDeclaration, serialName: String, doc: String?) =
    SerialType(
      serialName,
      TypeKind.SEALED,
      doc,
      subtypes =
        declaration
          .getSealedSubclasses()
          .filter { it.isSerializable() }
          .map { subtype -> subtype.serialName().also { discovered.putIfAbsent(it, subtype) } }
          .toList(),
      discriminated = hasPolymorphicBase(declaration),
    )

  private fun fieldsOf(declaration: KSClassDeclaration): List<SerialField> {
    val parameters = declaration.primaryConstructor?.parameters.orEmpty().associateBy { it.name }
    return declaration
      .getDeclaredProperties()
      .filter { isSerialized(it) }
      .map { property ->
        val parameter = parameters[property.simpleName]
        SerialField(
          name = property.serialNameAnnotation() ?: property.simpleName.asString(),
          type = typeOf(property),
          doc = property.doc(),
          range = property.range(),
          optional = parameter?.hasDefault ?: true,
        )
      }
      .toList()
  }

  private fun typeOf(property: KSPropertyDeclaration): TypeRef {
    val custom = property.annotation(AnnotationNames.SERIALIZABLE)?.argument("with")
    if (custom is KSType) return types.readSerializedWith(custom)
    return types.read(property.type.resolve())
  }

  private fun isSerialized(property: KSPropertyDeclaration): Boolean =
    property.hasBackingField &&
      property.annotation(AnnotationNames.TRANSIENT) == null &&
      property.extensionReceiver == null

  private fun isValueClass(declaration: KSClassDeclaration): Boolean =
    Modifier.VALUE in declaration.modifiers || Modifier.INLINE in declaration.modifiers

  /**
   * Whether a supertype other than `kotlin.*` and `java.*` ones is an interface or a class others
   * extend: the class then travels as one of that base's subtypes, with its serial name under
   * `type`.
   */
  private fun hasPolymorphicBase(declaration: KSClassDeclaration): Boolean =
    declaration.superTypes
      .map { it.resolve().declaration }
      .filterIsInstance<KSClassDeclaration>()
      .any { base ->
        val name = base.qualifiedName?.asString().orEmpty()
        !name.startsWith("kotlin.") && !name.startsWith("java.")
      }
}
