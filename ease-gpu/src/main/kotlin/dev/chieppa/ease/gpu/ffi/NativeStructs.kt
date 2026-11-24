package dev.chieppa.ease.gpu.ffi

import dev.chieppa.webgpu.ffi.WGPUChainedStruct
import dev.chieppa.webgpu.ffi.WGPUStringView
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/** webgpu.h's WGPU_STRLEN: a string view whose text ends with a zero byte. */
private const val STRLEN = -1L

/**
 * Points the string view embedded at [view] at [text], allocated in this arena; null is the null
 * view.
 */
internal fun Arena.setString(view: MemorySegment, text: String?) {
  WGPUStringView.data(view, if (text == null) MemorySegment.NULL else allocateFrom(text))
  WGPUStringView.length(view, STRLEN)
}

/** The text of the string view at [view]; empty for the null view. */
internal fun readString(view: MemorySegment): String {
  val data = WGPUStringView.data(view)
  if (data == MemorySegment.NULL) return ""
  val length = WGPUStringView.length(view)
  return if (length == STRLEN) {
    data.reinterpret(Long.MAX_VALUE).getString(0)
  } else {
    String(data.reinterpret(length).toArray(ValueLayout.JAVA_BYTE), Charsets.UTF_8)
  }
}

/** Fills the chained-struct header at the start of [struct] with [sType] and no next link. */
internal fun chain(struct: MemorySegment, sType: Int) {
  val header = struct.asSlice(0, WGPUChainedStruct.layout())
  WGPUChainedStruct.next(header, MemorySegment.NULL)
  WGPUChainedStruct.sType(header, sType)
}

/** One struct of [layout], zeroed, in this arena. */
internal fun Arena.struct(layout: MemoryLayout): MemorySegment = allocate(layout)

/**
 * A pointer argument wgpu kept only for the length of a callback, as an address that outlives it.
 */
internal fun kept(pointer: MemorySegment): MemorySegment =
  MemorySegment.ofAddress(pointer.address())
