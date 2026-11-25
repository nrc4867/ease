package dev.chieppa.ease.platform.input

private const val DIGIT_COUNT = 9
private const val SCANCODE_A = 4
private const val SCANCODE_1 = 30
private const val SCANCODE_0 = 39

/** Named keys and their SDL scancodes. */
private val NAMED_KEYS =
  listOf(
    "enter" to 40,
    "escape" to 41,
    "backspace" to 42,
    "tab" to 43,
    "space" to 44,
    "right" to 79,
    "left" to 80,
    "down" to 81,
    "up" to 82,
    "left_ctrl" to 224,
    "left_shift" to 225,
  )

/** The keys a [KeyboardDevice] reads, by control id and SDL scancode. */
private val KEYS: List<Pair<String, Int>> = buildList {
  for (letter in 'a'..'z') add("key.$letter" to SCANCODE_A + (letter - 'a'))
  for (digit in 1..DIGIT_COUNT) add("key.$digit" to SCANCODE_1 + digit - 1)
  add("key.0" to SCANCODE_0)
  for ((name, scancode) in NAMED_KEYS) add("key.$name" to scancode)
}

/**
 * The keyboard: letters, digits, arrows, space, enter, escape, backspace, tab, left control and
 * left shift, as `key.a`, `key.1`, `key.up`, `key.space`, `key.left_shift` and so on.
 */
public class KeyboardDevice(private val state: KeyboardState) :
  SampledDevice(KEYS.map { it.first }) {
  private val scancodes = KEYS.map { it.second }.toIntArray()

  override val kind: DeviceKind = DeviceKind.KEYBOARD
  override val name: String = "Keyboard"

  override fun read(index: Int): Float = if (state.isDown(scancodes[index])) 1f else 0f
}
