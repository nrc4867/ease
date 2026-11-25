package {{package}}

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.ecs.LocalTransform

/**
 * The {{nameWords}} system: runs each tick in the host's world for every entity that has a
 * [LocalTransform]. Clients receive what it changes as snapshots.
 */
public class {{Name}}System : IteratingSystem(family { all(LocalTransform) }) {
  override fun onTickEntity(entity: Entity) {
    // Read its components; change replicated ones through world.replication.write.
  }
}
