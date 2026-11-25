package dev.chieppa.ease.physics.volume

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc(
  "Makes a node's Collider a trigger: it blocks nothing and reports each VolumeSensor entering " +
    "and leaving it, on the world with authority over it. A trigger that fires once reports only " +
    "its first entry."
)
@Serializable
@SerialName("ease.TriggerVolume")
public class TriggerVolume : Component<TriggerVolume> {
  @Doc("Whether only the first entry is reported; later entries and every exit are not.")
  public var once: Boolean = false

  @Doc(
    "The collision layers, by name, of the sensors it reports; empty reports sensors on any layer."
  )
  public var layers: List<String> = emptyList()

  @Doc("Whether a trigger that fires once has fired.") public var fired: Boolean = false

  override fun type(): ComponentType<TriggerVolume> = TriggerVolume

  /** The component type. */
  public companion object : ComponentType<TriggerVolume>()
}
