package dev.chieppa.ease.ecs.replication

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.annotations.Range

/** A component using every replication option. */
@Replicated
internal class Lantern : Component<Lantern> {
  @Interpolated @Quantized(bits = 8) @Range(0.0, 1.0) var brightness: Float = 0f
  var fuel: Float = 0f
  @ReplicatedTo(Audience.OWNER_ONLY) var charges: Int = 0
  @ReplicatedTo(Audience.ALL_BUT_OWNER) var lit: Boolean = false

  /** Not replicated: a value, not a variable. */
  val color: Int = 0

  override fun type() = Lantern

  companion object : ComponentType<Lantern>()
}
