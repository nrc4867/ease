package dev.chieppa.ease.commands.wire

import dev.chieppa.ease.commands.WorldCodec

/**
 * How large a value in the compact wire form may be: its length in bytes, how deep it nests
 * (classes, collections and polymorphic values each count one) and how many elements one collection
 * holds. A writer and a reader under the same limits agree on what is valid, so what one writes the
 * other always reads.
 */
internal class Wire(val maxBytes: Int, val maxDepth: Int, val maxCollection: Int) {
  companion object {
    /** What a peer may send: hostile input is held tight. */
    val PEER: Wire = Wire(WorldCodec.MAX_COMMAND_BYTES, maxDepth = 32, maxCollection = 4096)

    /**
     * What the command log and undo history hold: written and read by the same process, so as large
     * as one change can reasonably be.
     */
    val LOG: Wire = Wire(64 * 1024 * 1024, maxDepth = 512, maxCollection = Int.MAX_VALUE)
  }
}
