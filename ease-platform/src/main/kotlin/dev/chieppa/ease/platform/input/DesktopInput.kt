package dev.chieppa.ease.platform.input

/** How many polls pass between looks for gamepads plugged in or out. */
public const val RESCAN_EVERY: Int = 60

/**
 * The keyboard and every connected gamepad as one [InputSource]. Gamepads are looked for every
 * [RESCAN_EVERY] polls, so a pad plugged in joins within about a second at 60 polls a second.
 */
public class DesktopInput(keyboardState: KeyboardState, private val provider: GamepadProvider) :
  InputSource {
  private val keyboard = KeyboardDevice(keyboardState)
  private val pads = mutableListOf<Pair<GamepadHandle, GamepadDevice>>()
  private var polls = 0

  override val devices: MutableList<InputDevice> = mutableListOf(keyboard)

  override fun poll() {
    if (polls % RESCAN_EVERY == 0) rescan()
    polls++
    provider.update()
    for (device in devices) device.poll()
  }

  private fun rescan() {
    val connected = provider.scan()
    val gone = pads.filter { (handle, _) -> connected.none { it === handle } }
    for (pair in gone) {
      pads.remove(pair)
      devices.remove(pair.second)
    }
    for (handle in connected) {
      if (pads.none { it.first === handle }) {
        val device = GamepadDevice(handle)
        pads += handle to device
        devices += device
      }
    }
  }
}
