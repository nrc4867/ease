package dev.chieppa.ease.codegen.model

/**
 * A `@Replicated` class as the processor's outputs see it, read from source by the readers in
 * `read` and checked by `ModelRules`: no KSP types, so outputs and rules are plain functions of it.
 */
internal data class ComponentModel(
  val packageName: String,
  /** The class's name inside its package, such as `Outer.Inner` for a nested class. */
  val className: String,
  /** The serial name: the class's `@SerialName`, or its qualified name without one. */
  val serialName: String,
  val fields: List<FieldModel>,
  /** How far the class can be seen, outer classes included. */
  val visibility: ClassVisibility = ClassVisibility.PUBLIC,
  val isComponent: Boolean = true,
  val hasTypeCompanion: Boolean = true,
  val hasNoArgumentConstructor: Boolean = true,
) {
  /** The class as code names it from anywhere. */
  val qualifiedName: String
    get() = if (packageName.isEmpty()) className else "$packageName.$className"

  /**
   * The generated serializer's name: the class's name inside its package, nested names joined by
   * `_`, with `Replicated` appended, so two nested classes of one name never collide.
   */
  val serializerName: String
    get() = className.replace('.', '_') + SERIALIZER_SUFFIX

  /** The mask with a bit for each field [selected] picks, the first field lowest. */
  fun maskOf(selected: (FieldModel) -> Boolean): Int =
    fields.foldIndexed(0) { index, mask, field ->
      if (selected(field)) mask or (1 shl index) else mask
    }

  /** The fields the entity's owner receives. */
  val ownerFields: Int
    get() = maskOf { it.audience != FieldAudience.ALL_BUT_OWNER }

  /** The fields every other peer receives. */
  val othersFields: Int
    get() = maskOf { it.audience != FieldAudience.OWNER_ONLY }

  /** The fields replicas blend. */
  val interpolatedFields: Int
    get() = maskOf { it.interpolated }

  companion object {
    const val SERIALIZER_SUFFIX = "Replicated"
  }
}
