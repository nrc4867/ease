package dev.chieppa.ease.importers.gltf

import com.github.quillraven.fleks.World
import dev.chieppa.ease.assets.ContentHash
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.Phase
import dev.chieppa.ease.ecs.phasedWorld
import dev.chieppa.ease.platform.files.FileChangeKind
import dev.chieppa.ease.render.ExtractSystem
import dev.chieppa.ease.render.RenderSnapshot
import dev.chieppa.ease.render.scene.MeshRef
import dev.chieppa.ease.scene.slots.AssetRef
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

/**
 * A model whose buffer is missing fails to import and its mesh to draw; when the buffer appears and
 * the store reloads it, the model imports and the mesh draws, with no problem left listed.
 */
class LateBufferTest {
  @TempDir lateinit var folder: Path

  @Test
  fun `a mesh whose buffer appears later imports, draws and stops being a problem`() {
    val project = ImportedProject(folder)
    val car = project.add("models/car_sedan.gltf")
    project.add("models/citybits_texture.png")
    val (store, imports) = project.open()
    val missing = imports.problems.single().message
    assertTrue("car_sedan.bin" in missing, missing)
    val snapshot = RenderSnapshot()
    val world: World = phasedWorld { add(Phase.EXTRACT, ExtractSystem(snapshot, assets = store)) }
    world.entity {
      it += LocalTransform()
      it += MeshRef(AssetRef(car.named(BODY_MESH)))
    }
    val extract = world.system<ExtractSystem>()
    world.update(DELTA)
    assertEquals(0, snapshot.count)
    assertEquals(1, extract.assetProblems.size)

    val buffer = project.add("models/car_sedan.bin")
    project.watcher.publish(folder.resolve("car_sedan.bin"), FileChangeKind.CREATED)
    store.database.sync()
    store.reload(buffer, ContentHash.of(folder.resolve("car_sedan.bin")))
    world.update(DELTA)

    assertEquals(emptyList(), imports.problems)
    assertEquals(emptyList(), extract.assetProblems.toList())
    assertEquals(1, snapshot.count)
  }

  private companion object {
    const val DELTA = 1f / 60f

    /** The cooked mesh of the car's body, named after the glTF mesh. */
    const val BODY_MESH = "Cube.13729.mesh"
  }
}
