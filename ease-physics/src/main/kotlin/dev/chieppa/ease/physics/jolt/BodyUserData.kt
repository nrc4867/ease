package dev.chieppa.ease.physics.jolt

/**
 * What a Jolt body's user data holds: its entity's id in the high half and its surface type's index
 * in the low half, so a query reads both from one lookup.
 */
internal object BodyUserData {
  fun of(entityId: Int, surface: Int): Long =
    (entityId.toLong() shl Int.SIZE_BITS) or (surface.toLong() and LOW_HALF)

  fun entityId(userData: Long): Int = (userData ushr Int.SIZE_BITS).toInt()

  fun surface(userData: Long): Int = userData.toInt()

  private const val LOW_HALF = 0xFFFF_FFFFL
}
