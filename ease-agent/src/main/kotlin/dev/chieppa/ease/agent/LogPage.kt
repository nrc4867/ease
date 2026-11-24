package dev.chieppa.ease.agent

/**
 * The [lines] recorded since a marker, oldest first; [next] is the marker to ask from next time,
 * and [dropped] counts the lines after the marker that were dropped before they were read.
 */
public class LogPage(
  public val lines: List<LogLine>,
  public val next: Long,
  public val dropped: Long,
)
