package dev.chieppa.ease.project.check

/**
 * The conflict markers git leaves in a file it could not merge: `<<<<<<<` opens our side, `=======`
 * opens theirs, `>>>>>>>` closes the block.
 */
public object ConflictMarkers {
  private const val OPEN = "<<<<<<< "
  private const val MIDDLE = "======="
  private const val CLOSE = ">>>>>>> "
  private const val BASE = "||||||| "
  private const val NEWLINE = '\n'

  /** Whether [text] holds a complete conflict block. */
  public fun isConflicted(text: String): Boolean = text.contains(OPEN) && sides(text, true) != null

  /** The text with every conflict block settled as our side, or null when a block is not closed. */
  public fun ours(text: String): String? = sides(text, true)

  /** The text with every conflict block settled as their side, or null when one is not closed. */
  public fun theirs(text: String): String? = sides(text, false)

  private fun sides(text: String, ours: Boolean): String? {
    val result = StringBuilder()
    var state = State.OUTSIDE
    var blocks = 0
    for (line in text.lineSequence()) {
      val next = transition(state, line)
      if (next != null) {
        if (next == State.OURS) blocks++
        state = next
        continue
      }
      val kept =
        when (state) {
          State.OUTSIDE -> true
          State.OURS -> ours
          State.THEIRS -> !ours
          State.BASE -> false
        }
      if (kept) result.append(line).append('\n')
    }
    return if (state == State.OUTSIDE && blocks > 0) result.toString().trimEnd(NEWLINE) else null
  }

  private fun transition(state: State, line: String): State? =
    when {
      state == State.OUTSIDE && line.startsWith(OPEN) -> State.OURS
      state == State.OURS && line.startsWith(BASE) -> State.BASE
      (state == State.OURS || state == State.BASE) && line == MIDDLE -> State.THEIRS
      state == State.THEIRS && line.startsWith(CLOSE) -> State.OUTSIDE
      else -> null
    }

  private enum class State {
    OUTSIDE,
    OURS,
    BASE,
    THEIRS,
  }
}
