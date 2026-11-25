package dev.chieppa.ease.memory

import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment

/** Frames a GPU works on while the next is recorded, unless told otherwise. */
public const val DEFAULT_FRAMES_IN_FLIGHT: Int = 2

/** Bytes each frame's block holds unless told otherwise: 4 MiB. */
public const val DEFAULT_BYTES_PER_FRAME: Long = 4L * 1024 * 1024

/** The alignment slices get unless asked for another: enough for any WGSL type. */
public const val DEFAULT_SLICE_ALIGNMENT: Long = 16L

/** Alignment of each frame's block, so any power-of-two slice alignment up to it holds. */
private const val BLOCK_ALIGNMENT = 256L

/**
 * Native memory for what a frame writes and hands to native calls: one block of [bytesPerFrame] per
 * frame in flight, handed out in slices by [take] and reset when its frame comes round again at
 * [beginFrame]. Slices are zeroed when taken.
 *
 * A [checked] arena (the default in a debug build) makes a new [FrameSlice] per [take], stamped
 * with its frame and site, and throws on a slice used after its frame. An unchecked arena reuses
 * its slice objects frame after frame, so a frame that takes the same slices as the last allocates
 * nothing on the heap. Use it from one thread at a time.
 */
public class FrameArena(
  public val framesInFlight: Int = DEFAULT_FRAMES_IN_FLIGHT,
  public val bytesPerFrame: Long = DEFAULT_BYTES_PER_FRAME,
  public val checked: Boolean = MemoryChecks.enabled,
) : AutoCloseable {
  init {
    require(framesInFlight > 0) { "Needs at least 1 frame in flight, got $framesInFlight" }
    require(bytesPerFrame > 0) { "Needs bytes for each frame, got $bytesPerFrame" }
  }

  private val arena = Arena.ofShared()
  private val blocks = Array(framesInFlight) { arena.allocate(bytesPerFrame, BLOCK_ALIGNMENT) }
  private val pools = Array(framesInFlight) { SlicePool(this) }
  private var block = 0
  private var used = 0L

  /** The frame under way: 0 until the first [beginFrame], then one more for each. */
  public var frame: Long = 0L
    private set

  /** Bytes taken in the frame under way, alignment padding included. */
  public val bytesUsed: Long
    get() = used

  /**
   * Starts the next frame: the block of the frame [framesInFlight] frames back is handed out again
   * from its start, and every slice taken before this frame goes stale.
   */
  public fun beginFrame() {
    frame++
    block = (frame % framesInFlight).toInt()
    used = 0L
    pools[block].reset()
  }

  /**
   * [bytes] zeroed bytes aligned to [alignment], a power of two up to 256, valid until the next
   * [beginFrame]; [site] names the taker in errors. Fails when the frame's block is full.
   */
  public fun take(
    bytes: Long,
    site: String,
    alignment: Long = DEFAULT_SLICE_ALIGNMENT,
  ): FrameSlice {
    require(bytes > 0) { "$site takes no bytes" }
    require(alignment in 1..BLOCK_ALIGNMENT && alignment and (alignment - 1) == 0L) {
      "$site asks for alignment $alignment, not a power of two up to $BLOCK_ALIGNMENT"
    }
    val offset = (used + alignment - 1) and (alignment - 1).inv()
    check(offset + bytes <= bytesPerFrame) {
      "$site needs $bytes bytes but frame $frame has ${bytesPerFrame - offset} of $bytesPerFrame " +
        "left: begin a frame for each frame drawn, or make the arena larger"
    }
    used = offset + bytes
    val slice = if (checked) FrameSlice(this) else pools[block].next()
    if (slice.offset != offset || slice.raw.byteSize() != bytes) {
      slice.raw = blocks[block].asSlice(offset, bytes)
      slice.offset = offset
    }
    slice.raw.fill(0)
    slice.frame = frame
    slice.site = site
    return slice
  }

  override fun close() {
    arena.close()
  }
}

/** A segment of [bytes] taken from [arena] for this frame, for callers that need only the bytes. */
public fun FrameArena.segment(bytes: Long, site: String): MemorySegment = take(bytes, site).segment
