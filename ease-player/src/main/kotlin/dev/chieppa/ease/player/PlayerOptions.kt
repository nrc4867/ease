package dev.chieppa.ease.player

import java.nio.file.Path

/**
 * What the player was asked to do on its command line. With [frames], it draws that many frames,
 * asks the window to resize halfway through, and exits: the smoke run CI makes. With
 * [headlessTicks], it opens no window and no GPU, runs that many ticks as fast as it can, and
 * exits: the headless run CI makes. With [content], the game's worlds load their assets and start
 * scene from that folder and follow its changes.
 */
internal data class PlayerOptions(
  val frames: Int? = null,
  val headlessTicks: Int? = null,
  val content: Path? = null,
) {
  companion object {
    private const val USAGE =
      "the player takes --frames <count> or --headless <ticks>, and --content <folder>"

    fun parse(args: Array<String>): PlayerOptions {
      var options = PlayerOptions()
      val rest = args.iterator()
      while (rest.hasNext()) {
        options =
          when (val arg = rest.next()) {
            "--frames" -> options.copy(frames = count(rest, arg))
            "--headless" -> options.copy(headlessTicks = count(rest, arg))
            "--content" -> options.copy(content = folder(rest, arg))
            else -> error("Unknown option $arg; $USAGE")
          }
      }
      check(options.frames == null || options.headlessTicks == null) { "Choose one: $USAGE" }
      return options
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
