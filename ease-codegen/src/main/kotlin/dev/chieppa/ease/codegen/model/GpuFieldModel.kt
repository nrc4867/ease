package dev.chieppa.ease.codegen.model

/** One field of a [GpuStructModel]: its Kotlin name and what it is on the GPU. */
internal data class GpuFieldModel(val name: String, val type: GpuFieldType)
