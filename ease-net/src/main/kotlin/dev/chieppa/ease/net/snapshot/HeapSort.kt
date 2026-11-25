package dev.chieppa.ease.net.snapshot

/** Sorts parallel arrays in place by key, allocating nothing. */
internal object HeapSort {
  /** Orders the first [count] entries of [ids] and [keys] by key, highest first. */
  fun descending(ids: IntArray, keys: FloatArray, count: Int) {
    for (start in count / 2 - 1 downTo 0) siftDown(ids, keys, start, count)
    for (end in count - 1 downTo 1) {
      swap(ids, keys, 0, end)
      siftDown(ids, keys, 0, end)
    }
  }

  /** Sinks entry [root] of a heap of [count] entries whose smallest key is on top. */
  private fun siftDown(ids: IntArray, keys: FloatArray, root: Int, count: Int) {
    var parent = root
    var left = parent * 2 + 1
    while (left < count) {
      val right = left + 1
      val child = if (right < count && keys[right] < keys[left]) right else left
      if (keys[parent] <= keys[child]) break
      swap(ids, keys, parent, child)
      parent = child
      left = parent * 2 + 1
    }
  }

  private fun swap(ids: IntArray, keys: FloatArray, a: Int, b: Int) {
    val id = ids[a]
    ids[a] = ids[b]
    ids[b] = id
    val key = keys[a]
    keys[a] = keys[b]
    keys[b] = key
  }
}
