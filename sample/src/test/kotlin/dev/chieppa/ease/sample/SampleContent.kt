package dev.chieppa.ease.sample

import dev.chieppa.ease.assets.loading.AssetStore
import dev.chieppa.ease.platform.files.FileChangeKind
import dev.chieppa.ease.platform.files.ManualFileWatcher
import dev.chieppa.ease.render.scene.Camera
import dev.chieppa.ease.render.scene.DirectionalLight
import dev.chieppa.ease.render.scene.Material
import dev.chieppa.ease.render.scene.MaterialRef
import dev.chieppa.ease.render.scene.Mesh
import dev.chieppa.ease.render.scene.MeshRef
import dev.chieppa.ease.runtime.GameContent
import dev.chieppa.ease.scene.document.SceneDocument
import dev.chieppa.ease.scene.document.SceneNode
import dev.chieppa.ease.scene.files.SceneFolder
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.resolve.ResolvedNode
import dev.chieppa.ease.scene.runtime.ResolvedScenes
import dev.chieppa.ease.scene.types.SceneTypes
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonPrimitive

/**
 * A copy of the sample's content folder in [root], changed by [prepare] (given the playfield scene
 * as its folder holds it, returning the scene to save in its place) and then opened as a host opens
 * it, but watched by a scripted [watcher], so a test edits the scene on disk and says when the edit
 * is seen.
 */
internal class SampleContent(
  root: Path,
  prepare: (SceneDocument) -> SceneDocument = { it },
) {
  /** The copied content folder. */
  val folder: Path =
    copy(source, root.resolve("content")).also { copied ->
      val scene = copied.resolve(PLAYFIELD)
      SceneFolder.write(scene, prepare(SceneFolder.read(scene)))
    }

  /** The watcher the content's database drains; [edit] publishes to it. */
  val watcher = ManualFileWatcher()

  /** The assets the worlds load, the playfield scene among them, opened on first use. */
  val assets: AssetStore by lazy { GameContent.open(folder, watcher) }

  /**
   * The playfield scene's nodes as the host places them: its own nodes and those of the prefabs it
   * places, such as the imported car's, from the files as the assets last read them.
   */
  fun placed(): List<ResolvedNode> =
    ResolvedScenes(TYPES, assets).resolve(SampleGame.PLAYFIELD, null).nodes

  /**
   * The keys of [document]'s nodes placed as the outermost scene, with the keys the prefabs it
   * places add, as text, sorted.
   */
  fun placedKeysOf(document: SceneDocument): List<String> {
    val nested = placed().map { it.key }.filter { it.depth > 1 }
    val placedIds = document.nodes.keys
    return (nested.filter { it.idAt(0) in placedIds }.map { it.text } + Companion.keysOf(document))
      .sorted()
  }

  /** The playfield scene as its folder holds it now. */
  fun playfield(): SceneDocument = SceneFolder.read(folder.resolve(PLAYFIELD))

  /** The playfield scene's nodes, as its folder holds them now. */
  fun nodes(): Collection<SceneNode> = playfield().nodes.values

  /** The playfield scene's cubes, its nodes with a velocity, as its folder holds them now. */
  fun cubes(): Collection<SceneNode> = nodes().filter { VELOCITY in it.components }

  /** The playfield scene's camera node, as its folder holds it now. */
  fun camera(): SceneNode = playfield().nodes.values.single { CAMERA in it.components }

  /** Saves [document] as the playfield scene and publishes every file it wrote or deleted. */
  fun edit(document: SceneDocument) {
    val scene = folder.resolve(PLAYFIELD).toAbsolutePath().normalize()
    val saved = SceneFolder.write(scene, document)
    (saved.written + saved.deleted).forEach {
      watcher.publish(scene.resolve(it), FileChangeKind.MODIFIED)
    }
  }

  companion object {
    /** The playfield scene's folder, relative to the content folder. */
    const val PLAYFIELD = "playfield.scene"

    private const val VELOCITY = "sample.Velocity"

    /** The component types the playfield and the car it places hold. */
    private val TYPES = SceneTypes.build {
      component(Mesh, Mesh.serializer())
      component(Material, Material.serializer())
      component(MeshRef, MeshRef.serializer())
      component(MaterialRef, MaterialRef.serializer())
      component(Camera, Camera.serializer())
      component(DirectionalLight, DirectionalLight.serializer())
      component(Velocity, Velocity.serializer())
    }
    private const val CAMERA = "ease.Camera"

    /** The keys of [document]'s nodes placed as the outermost scene, as text, sorted. */
    fun keysOf(document: SceneDocument): List<String> {
      val ids = document.nodes.keys
      return ids.map { NodeKey.of(emptyList(), it).text }.sorted()
    }

    /** The number [field] of [node]'s component [component], as its node file holds it. */
    fun field(node: SceneNode, component: String, field: String): Float {
      val fields = node.components.getValue(component)
      return fields.getValue(field).jsonPrimitive.float
    }

    /** The sample's own content folder, which Gradle names. */
    val source: Path = Path.of(checkNotNull(System.getProperty("ease.sampleContent")))

    /** Copies every file under [from] to [to], leaving out the per-machine `.ease` caches. */
    private fun copy(from: Path, to: Path): Path {
      Files.walk(from).use { walk ->
        walk
          .filter { Files.isRegularFile(it) }
          .filter { file -> from.relativize(file).none { it.toString() == ".ease" } }
          .forEach { file ->
            val target = to.resolve(from.relativize(file).toString())
            target.parent.createDirectories()
            file.copyTo(target)
          }
      }
      return to
    }
  }
}
