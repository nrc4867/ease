package dev.chieppa.ease.gpu

/**
 * What a device allows that callers size work by: the largest buffer, in bytes, and the largest
 * range of a buffer one storage binding may cover.
 */
public data class GpuLimits(val maxBufferSize: Long, val maxStorageBufferBindingSize: Long)
