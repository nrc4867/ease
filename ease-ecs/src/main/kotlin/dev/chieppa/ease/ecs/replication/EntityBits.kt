package dev.chieppa.ease.ecs.replication

private const val WORD_SHIFT = 6
private const val INITIAL_WORDS = 8

/** One bit per entity id, growing as ids grow and allocating nothing once grown. */
internal class EntityBits {
  private var words = LongArray(INITIAL_WORDS)

  operator fun get(index: Int): Boolean {
    val word = index ushr WORD_SHIFT
    return word < words.size && (words[word] and (1L shl index)) != 0L
  }

  fun set(index: Int) {
    val word = index ushr WORD_SHIFT
    if (word >= words.size) words = words.copyOf(maxOf(word + 1, words.size * 2))
    words[word] = words[word] or (1L shl index)
  }

  fun clearAll() {
    words.fill(0L)
  }
}
