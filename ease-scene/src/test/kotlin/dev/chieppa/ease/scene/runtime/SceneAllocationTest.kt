package dev.chieppa.ease.scene.runtime

import com.github.quillraven.fleks.Entity
import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.meta.AssetMeta
import dev.chieppa.ease.assets.meta.MetaFiles
import dev.chieppa.ease.scene.Scenes.document
import dev.chieppa.ease.scene.Scenes.node
import dev.chieppa.ease.scene.document.Instance
import dev.chieppa.ease.scene.files.SceneFolder
import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.testing.AllocationBudget
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class SceneAllocationTest {
  @TempDir lateinit var folder: Path

  private val caveGuid = Guid(0, 1)

  private fun placedHost(): ScenePeer {
    val crate = folder.resolve("crate.prefab")
    SceneFolder.write(crate, document(node(10, "Box"), node(11, "Lid", parent = 10)))
    MetaFiles.write(MetaFiles.metaOf(crate), AssetMeta(guid = Guid(0, 2)))
    val cave = folder.resolve("cave.scene")
    val nodes =
      (1L..50L).map {
        node(it, "Node $it", instance = if (it % 5 == 0L) Instance(Guid(0, 2)) else null)
      }
    SceneFolder.write(cave, document(*nodes.toTypedArray()))
    MetaFiles.write(MetaFiles.metaOf(cave), AssetMeta(guid = caveGuid))
    return ScenePeer(folder).also { it.scenes.place(caveGuid, tick = 0) }
  }

  @Test
  fun `looking a node up by its key allocates nothing`() {
    val host = placedHost()
    val index = host.world.system<SceneNodes>().index
    val keys = listOf(NodeKey.of(LocalId(7)), NodeKey.of(listOf(LocalId(10)), LocalId(11)))
    var found = 0

    AllocationBudget.assertNoAllocation {
      for (i in keys.indices) if (index[keys[i]] != Entity.NONE) found++
    }

    assertTrue(found > 0)
  }

  @Test
  fun `the per-tick asset and scene check allocates nothing when no file changed`() {
    val host = placedHost()
    var tick = 1L

    AllocationBudget.assertNoAllocation { host.beforeTick(tick++) }
  }
}
