package dev.chieppa.ease.memory

import java.lang.foreign.MemorySegment

/**
 * Native bytes a [FrameArena] handed out for one frame: valid until the arena's next
 * [FrameArena.beginFrame]. A checked arena stamps each slice with its [frame] and the [site] that
 * took it, and [segment] throws [StaleSliceException] once another frame has begun.
 */
public class FrameSlice internal constructor(private val arena: FrameArena) {
  internal var raw: MemorySegment = MemorySegment.NULL
  internal var offset: Long = -1L

  /** The frame this slice was taken in. */
  public var frame: Long = 0L
    internal set

  /** Where the slice was taken, as its taker named it. */
  public var site: String = ""
    internal set

  /** The slice's length in bytes. */
  public val byteSize: Long
    get() = raw.byteSize()

  /** The bytes; in a checked arena, throws [StaleSliceException] after the frame has ended. */
  public val segment: MemorySegment
    get() {
      checkCurrent()
      return raw
    }

  /** Throws [StaleSliceException] when this slice's frame is over and the arena checks. */
  public fun checkCurrent() {
    if (arena.checked && frame != arena.frame) throw StaleSliceException(site, frame, arena.frame)
  }
}

/** A frame slice was used after its frame ended. */
public class StaleSliceException(site: String, taken: Long, current: Long) :
  IllegalStateException(
    "A frame slice taken at $site in frame $taken was used in frame $current, after its frame"
  )
