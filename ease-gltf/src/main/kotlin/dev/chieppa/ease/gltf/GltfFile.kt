package dev.chieppa.ease.gltf

import dev.chieppa.ease.gltf.json.GltfRoot
import dev.chieppa.ease.memory.MappedRegion
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Opens the files a glTF file names beside it, by the relative path its URI decodes to, as a
 * little-endian region; throws when there is no such file.
 */
public fun interface SiblingFiles {
  /** The bytes of the file at [relativePath], from the glTF file's folder. */
  public fun open(relativePath: String): MappedRegion
}

/**
 * A glTF 2.0 file read: its JSON [root], its buffers loaded (from the GLB binary chunk, data URIs
 * or files beside it), its [accessors], its images' bytes and its punctual [lights]. Reading checks
 * the version and the required extensions; everything else is checked as it is read, each failure a
 * [GltfException] naming the element.
 */
public class GltfFile private constructor(public val root: GltfRoot, buffers: List<MappedRegion>) {
  private val views = BufferViews(root, buffers)

  /** The file's accessors. */
  public val accessors: Accessors = Accessors(root, views)

  /** The `KHR_lights_punctual` lights, by index; empty when the file has none. */
  public val lights: List<PunctualLight> = PunctualLights.of(root)

  /** The light node [node] carries, or null when it carries none. */
  public fun lightOf(node: Int): PunctualLight? =
    PunctualLights.indexOf(root.nodes[node], "nodes[$node]")?.let { index ->
      lights.getOrNull(index)
        ?: fail("nodes[$node].extensions", "names light $index of ${lights.size}")
    }

  /** The encoded bytes of image [index] (PNG or JPEG), and its MIME type when the file says. */
  public fun image(index: Int, files: SiblingFiles): ImageBytes {
    val path = "images[$index]"
    val image =
      root.images.getOrNull(index) ?: fail(path, "the file has ${root.images.size} images")
    val uri = image.uri
    return when {
      image.bufferView != null -> {
        val region = views.whole(image.bufferView)
        ImageBytes(region.copyBytes(0, region.byteSize.toInt()), image.mimeType)
      }
      uri == null -> fail(path, "has neither a uri nor a bufferView")
      Uris.isData(uri) -> ImageBytes(Uris.dataBytes(uri, path), Uris.dataMimeType(uri))
      else -> {
        val region = openSibling(files, uri, path)
        ImageBytes(region.copyBytes(0, region.byteSize.toInt()), image.mimeType ?: mimeOf(uri))
      }
    }
  }

  public companion object {
    private val json = Json {
      ignoreUnknownKeys = true
      isLenient = false
    }

    /** The extensions a file may require and still be read. */
    public val SUPPORTED_EXTENSIONS: Set<String> =
      setOf(PunctualLights.EXTENSION, "KHR_materials_emissive_strength")

    /**
     * Reads [bytes], a `.gltf` or `.glb` called [name], opening the files its URIs name through
     * [files]. Throws [GltfException] naming the element at fault.
     */
    public fun read(bytes: ByteArray, name: String, files: SiblingFiles): GltfFile {
      val glb = if (Glb.isGlb(bytes)) Glb.split(bytes, name) else null
      val text = glb?.json ?: String(bytes, Charsets.UTF_8)
      val root = parse(text, name)
      if (!root.asset.version.startsWith("2.")) {
        fail("asset.version", "${root.asset.version} is not glTF 2")
      }
      root.extensionsRequired
        .firstOrNull { it !in SUPPORTED_EXTENSIONS }
        ?.let { fail("extensionsRequired", "$it is not supported") }
      val buffers = root.buffers.indices.map { loadBuffer(root, it, glb?.binary, files) }
      return GltfFile(root, buffers)
    }

    private fun parse(text: String, name: String): GltfRoot =
      try {
        json.decodeFromString<GltfRoot>(text)
      } catch (failure: SerializationException) {
        throw GltfException(name, "is not glTF JSON: ${failure.message}", failure)
      } catch (failure: IllegalArgumentException) {
        throw GltfException(name, "is not glTF JSON: ${failure.message}", failure)
      }

    private fun loadBuffer(
      root: GltfRoot,
      index: Int,
      binary: MappedRegion?,
      files: SiblingFiles,
    ): MappedRegion {
      val path = "buffers[$index]"
      val buffer = root.buffers[index]
      val uri = buffer.uri
      val region =
        when {
          uri == null ->
            binary?.takeIf { index == 0 }
              ?: fail(path, "has no uri and is not a GLB's binary chunk")
          Uris.isData(uri) -> MappedRegion.of(Uris.dataBytes(uri, path), path)
          else -> openSibling(files, uri, path)
        }
      if (buffer.byteLength > region.byteSize) {
        fail(
          path,
          "declares ${buffer.byteLength} bytes, but only ${region.byteSize} were found",
        )
      }
      return region.region(0, buffer.byteLength)
    }

    @Suppress("TooGenericExceptionCaught") // The opener is the caller's; any failure is data.
    private fun openSibling(files: SiblingFiles, uri: String, path: String): MappedRegion =
      try {
        files.open(Uris.relativePath(uri))
      } catch (failure: RuntimeException) {
        throw GltfException(path, "cannot open '$uri': ${failure.message}", failure)
      } catch (failure: java.io.IOException) {
        throw GltfException(path, "cannot open '$uri': ${failure.message}", failure)
      }

    private fun mimeOf(uri: String): String? =
      when (uri.substringAfterLast('.').lowercase()) {
        "png" -> "image/png"
        "jpg",
        "jpeg" -> "image/jpeg"
        else -> null
      }
  }
}

/** An image's encoded bytes and MIME type, when known. */
public class ImageBytes(public val bytes: ByteArray, public val mimeType: String?)
