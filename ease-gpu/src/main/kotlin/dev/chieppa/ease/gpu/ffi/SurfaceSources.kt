package dev.chieppa.ease.gpu.ffi

import dev.chieppa.ease.platform.SurfaceHandle
import dev.chieppa.webgpu.ffi.WGPUSurfaceDescriptor
import dev.chieppa.webgpu.ffi.WGPUSurfaceSourceMetalLayer
import dev.chieppa.webgpu.ffi.WGPUSurfaceSourceWaylandSurface
import dev.chieppa.webgpu.ffi.WGPUSurfaceSourceWindowsHWND
import dev.chieppa.webgpu.ffi.WGPUSurfaceSourceXlibWindow
import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment

/** A wgpu surface for the window [handle] names, made on [instance]. */
internal fun createSurface(instance: MemorySegment, handle: SurfaceHandle): MemorySegment =
  Arena.ofConfined().use { arena ->
    val descriptor = arena.struct(WGPUSurfaceDescriptor.layout())
    WGPUSurfaceDescriptor.nextInChain(descriptor, source(arena, handle))
    wgpu_h.wgpuInstanceCreateSurface(instance, descriptor).also {
      check(it != MemorySegment.NULL) { "wgpu could not make a surface for $handle" }
    }
  }

private fun source(arena: Arena, handle: SurfaceHandle): MemorySegment =
  when (handle) {
    is SurfaceHandle.Win32 ->
      arena.struct(WGPUSurfaceSourceWindowsHWND.layout()).also {
        chain(it, wgpu_h.WGPUSType_SurfaceSourceWindowsHWND())
        WGPUSurfaceSourceWindowsHWND.hinstance(it, MemorySegment.ofAddress(handle.hinstance))
        WGPUSurfaceSourceWindowsHWND.hwnd(it, MemorySegment.ofAddress(handle.hwnd))
      }
    is SurfaceHandle.Metal ->
      arena.struct(WGPUSurfaceSourceMetalLayer.layout()).also {
        chain(it, wgpu_h.WGPUSType_SurfaceSourceMetalLayer())
        WGPUSurfaceSourceMetalLayer.layer(it, MemorySegment.ofAddress(handle.layer))
      }
    is SurfaceHandle.Xlib ->
      arena.struct(WGPUSurfaceSourceXlibWindow.layout()).also {
        chain(it, wgpu_h.WGPUSType_SurfaceSourceXlibWindow())
        WGPUSurfaceSourceXlibWindow.display(it, MemorySegment.ofAddress(handle.display))
        WGPUSurfaceSourceXlibWindow.window(it, handle.window)
      }
    is SurfaceHandle.Wayland ->
      arena.struct(WGPUSurfaceSourceWaylandSurface.layout()).also {
        chain(it, wgpu_h.WGPUSType_SurfaceSourceWaylandSurface())
        WGPUSurfaceSourceWaylandSurface.display(it, MemorySegment.ofAddress(handle.display))
        WGPUSurfaceSourceWaylandSurface.surface(it, MemorySegment.ofAddress(handle.surface))
      }
  }
