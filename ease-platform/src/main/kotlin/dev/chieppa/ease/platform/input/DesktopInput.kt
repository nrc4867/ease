package dev.chieppa.ease.platform.input

/** How many polls pass between looks for gamepads plugged in or out. */
public const val RESCAN_EVERY: Int = 60

/**
 * The keyboard and every connected gamepad as one [InputSource]. Gamepads are looked for every
 * [RESCAN_EVERY] polls, so a pad plugged in joins within about a second at 60 polls a second.
 * Polling allocates nothing while the same gamepads stay connected; a pad joining or leaving
 * allocates on the poll that notices it.
 */
public class DesktopInput(keyboardState: KeyboardState, private val provider: GamepadProvider) :
  InputSource {
  private val handles = ArrayList<GamepadHandle>()

  override val devices: MutableList<InputDevice> =
    ArrayList<InputDevice>().also {
      it += KeyboardDevice(keyboardState)
    }

  private var polls = 0

  override fun poll() {
    if (polls % RESCAN_EVERY == 0) rescan()
    polls++
    provider.update()
    for (index in devices.indices) devices[index].poll()
  }

  private fun rescan() {
    val connected = provider.scan()
    if (unchanged(connected)) return
    handles.clear()
    while (devices.size > 1) devices.removeAt(devices.lastIndex)
    for (handle in connected) {
      handles += handle
      devices += GamepadDevice(handle)
    }
  }

  private fun unchanged(connected: List<GamepadHandle>): Boolean {
    if (connected.size != handles.size) return false
    var same = true
    for (index in 0 until handles.size) if (connected[index] !== handles[index]) same = false
    return same
  }
}
