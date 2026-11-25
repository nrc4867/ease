package dev.chieppa.ease.player

import java.nio.file.Path

/**
 * What the player was asked to do on its command line. With [frames], it draws that many frames,
 * asks the window to resize halfway through, and exits: the smoke run CI makes. With
 * [headlessTicks], it opens no window and no GPU, runs that many ticks as fast as it can, and
 * exits: the headless run CI makes. With [content], the game's worlds load their assets and start
 * scene from that folder and follow its changes. A window opens [width] by [height] screen units,
 * 1280 by 800 unless `--size <width>x<height>` says otherwise.
 */
internal data class PlayerOptions(
  val frames: Int? = null,
  val headlessTicks: Int? = null,
  val content: Path? = null,
  val width: Int = DEFAULT_WIDTH,
  val height: Int = DEFAULT_HEIGHT,
) {
  companion object {
    private const val USAGE =
      "the player takes --frames <count> or --headless <ticks>, --content <folder> and " +
        "--size <width>x<height>"
    const val DEFAULT_WIDTH = 1280
    const val DEFAULT_HEIGHT = 800

    fun parse(args: Array<String>): PlayerOptions {
      var options = PlayerOptions()
      val rest = args.iterator()
      while (rest.hasNext()) {
        options =
          when (val arg = rest.next()) {
            "--frames" -> options.copy(frames = count(rest, arg))
            "--headless" -> options.copy(headlessTicks = count(rest, arg))
            "--content" -> options.copy(content = folder(rest, arg))
            "--size" -> size(rest, arg).let { (w, h) -> options.copy(width = w, height = h) }
            else -> error("Unknown option $arg; $USAGE")
          }
      }
      check(options.frames == null || options.headlessTicks == null) { "Choose one: $USAGE" }
      return options
    }

    private fun size(rest: Iterator<String>, option: String): Pair<Int, Int> {
      check(rest.hasNext()) { "$option needs a size; $USAGE" }
      val text = rest.next()
      val parts = text.split('x').mapNotNull { it.toIntOrNull() }
      check(parts.size == 2 && parts.all { it > 0 }) { "$option needs <width>x<height>, got $text" }
      return parts[0] to parts[1]
    }

    private fun folder(rest: Iterator<String>, option: String): Path {
      check(rest.hasNext()) { "$option needs a folder; $USAGE" }
      return Path.of(rest.next())
    }

    private fun count(rest: Iterator<String>, option: String): Int {
      check(rest.hasNext()) { "$option needs a count; $USAGE" }
      val count = rest.next().toInt()
      check(count > 0) { "$option needs a positive count, got $count" }
      return count
    }
  }
}
