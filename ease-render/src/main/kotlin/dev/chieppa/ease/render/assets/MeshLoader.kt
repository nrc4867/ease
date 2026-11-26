package dev.chieppa.ease.render.assets

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.loading.AssetLoader
import dev.chieppa.ease.memory.MappedFile
import dev.chieppa.ease.memory.MappedRegion
import java.nio.file.Path
import kotlin.reflect.KClass

/** Loads a cooked `.mesh` file, mapping it and copying its vertices and indices into a mesh. */
public object MeshLoader : AssetLoader<MeshAsset> {
  override val type: KClass<MeshAsset> = MeshAsset::class

  override fun load(guid: Guid, bytes: ByteArray): MeshAsset =
    MeshFormat.read(MappedRegion.of(bytes, "mesh $guid"))

  override fun load(guid: Guid, path: Path): MeshAsset =
    MappedFile.open(path).use { MeshFormat.read(it.whole) }
}
