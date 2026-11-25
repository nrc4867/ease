package dev.chieppa.ease.gpu.lit

import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.memory.DEFAULT_SLICE_ALIGNMENT
import dev.chieppa.ease.memory.MAT4_FLOATS
import dev.chieppa.ease.memory.NativeWriter
import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.MemorySegment

private const val FRAME_SITE = "LitMeshPass.frame"
private const val VIEW_SITE = "LitMeshPass.view"
private const val MATERIALS_SITE = "LitMeshPass.materials"
private const val OBJECTS_SITE = "LitMeshPass.objects"

/**
 * Writes a [MeshFrame]'s uniforms and per-draw data through the generated struct writers into
 * slices of [context]'s frame arena, then hands each slice to the queue: no heap copy, no objects.
 */
internal class FrameUploads(private val context: GpuContext, private val writer: NativeWriter) {
  /** The sun and ambient light. */
  fun frame(buffer: MemorySegment, frame: MeshFrame) {
    val slice = context.frames.take(FrameDataWriter.STRUCT_SIZE, FRAME_SITE)
    writer.begin(slice)
    FrameDataWriter.light(writer) {
      val direction = frame.lightDirection
      val color = frame.lightColor
      DirectionalLightDataWriter.direction(writer, direction[0], direction[1], direction[2])
      DirectionalLightDataWriter.intensity(writer, frame.lightIntensity)
      DirectionalLightDataWriter.color(writer, color[0], color[1], color[2])
    }
    val ambient = frame.ambient
    FrameDataWriter.ambient(writer, ambient[0], ambient[1], ambient[2])
    upload(buffer, slice.segment, FrameDataWriter.STRUCT_SIZE)
  }

  /** The camera. */
  fun view(buffer: MemorySegment, frame: MeshFrame) {
    val slice = context.frames.take(ViewDataWriter.STRUCT_SIZE, VIEW_SITE)
    writer.begin(slice)
    ViewDataWriter.viewProjection(writer, frame.viewProjection)
    val eye = frame.cameraPosition
    ViewDataWriter.cameraPosition(writer, eye[0], eye[1], eye[2])
    upload(buffer, slice.segment, ViewDataWriter.STRUCT_SIZE)
  }

  /** Each draw's material into [materials] and transforms into [objects]. */
  fun draws(materials: MemorySegment, objects: MemorySegment, frame: MeshFrame) {
    val count = frame.count
    if (count == 0) return
    val materialBytes = count * LitMaterialWriter.STRUCT_SIZE
    val materialSlice = context.frames.take(materialBytes, MATERIALS_SITE)
    writer.begin(materialSlice)
    val values = frame.materials
    for (index in 0 until count) {
      val at = index * MaterialFloats.COUNT
      LitMaterialWriter.baseColor(writer, values[at], values[at + 1], values[at + 2])
      LitMaterialWriter.roughness(writer, values[at + MaterialFloats.ROUGHNESS])
      LitMaterialWriter.metallic(writer, values[at + MaterialFloats.METALLIC])
      val emissive = at + MaterialFloats.EMISSIVE
      LitMaterialWriter.emissive(
        writer,
        values[emissive],
        values[emissive + 1],
        values[emissive + 2],
      )
      writer.next(LitMaterialWriter.STRUCT_SIZE)
    }
    upload(materials, materialSlice.segment, materialBytes)
    val objectBytes = count * ObjectDataWriter.STRUCT_SIZE
    val objectSlice = context.frames.take(objectBytes, OBJECTS_SITE)
    writer.begin(objectSlice)
    for (index in 0 until count) {
      ObjectDataWriter.model(writer, frame.models, index * MAT4_FLOATS)
      ObjectDataWriter.normalMatrix(writer, frame.normals, index * MAT4_FLOATS)
      writer.next(ObjectDataWriter.STRUCT_SIZE)
    }
    upload(objects, objectSlice.segment, objectBytes)
  }

  private fun upload(buffer: MemorySegment, data: MemorySegment, bytes: Long) {
    wgpu_h.wgpuQueueWriteBuffer(context.queue, buffer, 0, data, bytes)
  }

  companion object {
    /** The most arena bytes one frame of up to [capacity] draws takes, alignment included. */
    fun bytesPerFrame(capacity: Int): Long =
      aligned(FrameDataWriter.STRUCT_SIZE) +
        aligned(ViewDataWriter.STRUCT_SIZE) +
        aligned(capacity * LitMaterialWriter.STRUCT_SIZE) +
        aligned(capacity * ObjectDataWriter.STRUCT_SIZE)

    private fun aligned(bytes: Long): Long =
      (bytes + DEFAULT_SLICE_ALIGNMENT - 1) / DEFAULT_SLICE_ALIGNMENT * DEFAULT_SLICE_ALIGNMENT
  }
}
