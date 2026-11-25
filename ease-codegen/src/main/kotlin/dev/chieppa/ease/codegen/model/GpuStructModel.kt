package dev.chieppa.ease.codegen.model

/**
 * A `@GpuStruct` class as the processor reads it: where it lives, how far it can be seen, and its
 * fields in declaration order.
 */
internal data class GpuStructModel(
  val packageName: String,
  /** The class's name inside its package, outer classes first, joined by dots. */
  val className: String,
  val fields: List<GpuFieldModel>,
  val visibility: ClassVisibility = ClassVisibility.PUBLIC,
) {
  /** The struct's name in WGSL: the class's own simple name. */
  val wgslName: String
    get() = className.substringAfterLast('.')

  /** The generated writer object's name: the class path joined by underscores, then `Writer`. */
  val writerName: String
    get() = className.replace('.', '_') + "Writer"

  /** The writer's qualified name, for structs that nest this one. */
  val qualifiedWriterName: String
    get() = if (packageName.isEmpty()) writerName else "$packageName.$writerName"
}
