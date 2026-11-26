package dev.chieppa.ease.textures.bc

import java.util.stream.IntStream

/**
 * Runs work for each row of 4x4 blocks in parallel, on the fork-join pool the caller runs in (the
 * common pool unless the caller is a task of another pool). Each row writes only its own part of
 * the output, so the bytes never depend on how the rows were scheduled.
 */
public object BlockRows {
  /** Calls [body] once for every row from 0 until [rows], in any order and on any thread. */
  public fun run(rows: Int, body: (Int) -> Unit) {
    IntStream.range(0, rows).parallel().forEach { body(it) }
  }
}
