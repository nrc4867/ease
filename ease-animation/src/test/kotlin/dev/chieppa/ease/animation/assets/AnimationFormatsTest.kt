package dev.chieppa.ease.animation.assets

import dev.chieppa.ease.animation.TestRig
import dev.chieppa.ease.assets.Guid
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/** Skins, clips with their events and animator assets read back what was written. */
class AnimationFormatsTest {
  private val guid = Guid(0, 1)

  @Test
  fun `skins and clips read back what was written, clip events included`() {
    val skin =
      SkinAsset(listOf("00000000000000aa"), listOf("hip"), FloatArray(16) { it * 1f }, null)
    val clip =
      ClipAsset(
        "wave",
        1.5f,
        listOf(
          ClipChannel(
            "00000000000000aa",
            ChannelPath.ROTATION,
            Interpolation.LINEAR,
            floatArrayOf(0f, 1.5f),
            FloatArray(8),
          )
        ),
        listOf(ClipEvent("peak", 0.75f)),
      )

    val skinRead = SkinFormat.load(guid, SkinFormat.write(skin))
    val clipRead = ClipFormat.load(guid, ClipFormat.write(clip))

    assertEquals(skin.joints, skinRead.joints)
    assertContentEquals(skin.inverseBindMatrices, skinRead.inverseBindMatrices)
    assertEquals("wave", clipRead.name)
    assertEquals(ChannelPath.ROTATION, clipRead.channels.single().path)
    assertContentEquals(floatArrayOf(0f, 1.5f), clipRead.channels.single().times)
    assertEquals("peak", clipRead.events.single().name)
    assertEquals(0.75f, clipRead.events.single().time)
  }

  @Test
  fun `an animator asset is canonical text that reads back`() {
    val bytes = AnimatorFormat.write(TestRig.animator())
    val read = AnimatorFormat.read(bytes)

    assertContentEquals(bytes, AnimatorFormat.write(read))
    assertEquals(listOf("idle", "move"), read.states.map { it.name })
    assertEquals(TestRig.WALK, read.states[1].blend?.points?.get(1)?.clip)
    assertEquals("footstepLeft", read.probes[0].event)
  }
}
