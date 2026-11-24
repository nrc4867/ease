package dev.chieppa.ease.agent

/** Lines the log keeps unless told otherwise. */
public const val DEFAULT_LOG_CAPACITY: Int = 1_000

/**
 * What the host did, as numbered lines kept in memory: the newest [capacity] of them. Each line
 * also goes to [echo] as it is recorded. Safe to use from any thread.
 */
public class HostLog(
  private val capacity: Int = DEFAULT_LOG_CAPACITY,
  private val echo: (LogLine) -> Unit = {},
) {
  init {
    require(capacity > 0) { "A log keeps at least one line, got $capacity" }
  }

  private val lines = ArrayDeque<LogLine>()
  private var next = 0L

  /** Records [text] as the next line and returns its sequence number. */
  public fun record(text: String): Long {
    val line =
      synchronized(this) {
        LogLine(next++, text).also {
          lines.addLast(it)
          if (lines.size > capacity) lines.removeFirst()
        }
      }
    echo(line)
    return line.sequence
  }

  /** At most [limit] lines from sequence number [marker] on, oldest first. */
  @Synchronized
  public fun since(marker: Long, limit: Int = capacity): LogPage {
    require(marker >= 0) { "A marker is 0 or more, got $marker" }
    val oldest = lines.firstOrNull()?.sequence ?: next
    val taken = lines.filter { it.sequence >= marker }.take(limit)
    val resume = taken.lastOrNull()?.let { it.sequence + 1 } ?: maxOf(marker, oldest)
    return LogPage(taken, resume, maxOf(0L, oldest - marker))
  }
}
