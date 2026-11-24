package dev.chieppa.ease.player

/**
 * What the player was asked to do on its command line. With [frames], it draws that many frames,
 * asks the window to resize halfway through, and exits: the smoke run CI makes.
 */
internal data class PlayerOptions(val frames: Int?) {
  companion object {
    fun parse(args: Array<String>): PlayerOptions {
      var frames: Int? = null
      val rest = args.iterator()
      while (rest.hasNext()) {
        when (val arg = rest.next()) {
          "--frames" -> frames = rest.next().toInt()
          else -> error("Unknown option $arg; the player takes --frames <count>")
        }
      }
      return PlayerOptions(frames)
    }
  }
}
