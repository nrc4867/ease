package dev.chieppa.ease.gltf

import java.net.URLDecoder
import java.util.Base64

/** The URIs a glTF file names its buffers and images by. */
public object Uris {
  private const val DATA = "data:"
  private const val BASE64 = ";base64,"

  /** Whether [uri] carries its bytes itself. */
  public fun isData(uri: String): Boolean = uri.startsWith(DATA)

  /** The bytes of the base64 data URI [uri]; throws [GltfException] naming [element] otherwise. */
  public fun dataBytes(uri: String, element: String): ByteArray {
    val marker = uri.indexOf(BASE64)
    if (!isData(uri) || marker < 0) {
      fail(element, "only base64 data URIs are read")
    }
    return try {
      Base64.getDecoder().decode(uri.substring(marker + BASE64.length))
    } catch (failure: IllegalArgumentException) {
      throw GltfException(element, "the data URI is not valid base64", failure)
    }
  }

  /** The MIME type a data URI declares, or null when it declares none. */
  public fun dataMimeType(uri: String): String? {
    val header = uri.removePrefix(DATA).substringBefore(';')
    return header.substringBefore(',').ifEmpty { null }
  }

  /** The relative path [uri] names, its percent escapes decoded. */
  public fun relativePath(uri: String): String =
    URLDecoder.decode(uri.replace("+", "%2B"), Charsets.UTF_8)
}
