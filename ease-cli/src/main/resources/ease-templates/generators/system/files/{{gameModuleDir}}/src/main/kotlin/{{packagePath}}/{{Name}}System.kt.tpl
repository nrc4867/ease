package {{package}}

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.ecs.LocalTransform

/** The {{nameWords}} system: runs each tick for every entity that has a [LocalTransform]. */
public class {{Name}}System : IteratingSystem(family { all(LocalTransform) }) {
  override fun onTickEntity(entity: Entity) {
    // Read and write the entity's components here.
  }
}
