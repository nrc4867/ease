package dev.chieppa.ease.codegen.model

/**
 * One field as laid out: its byte [offset], the [size] and [alignment] it takes, its WGSL type, and
 * the attributes its declaration carries, null where WGSL's defaults already give them.
 */
internal data class GpuFieldLayout(
  val field: GpuFieldModel,
  val offset: Long,
  val size: Long,
  val alignment: Long,
  val wgslType: String,
  val alignAttribute: Long? = null,
  val sizeAttribute: Long? = null,
)
