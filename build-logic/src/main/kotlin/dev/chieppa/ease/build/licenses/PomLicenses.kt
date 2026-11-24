package dev.chieppa.ease.build.licenses

import java.io.File
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/** What a POM says about licensing: its own licenses and, when it has one, its parent POM. */
data class PomLicenses(val licenses: List<DeclaredLicense>, val parent: String?) {
  companion object {
    /** Reads [pom]. [parent] is `group:name:version`. */
    fun read(pom: File): PomLicenses {
      val factory =
        DocumentBuilderFactory.newInstance().apply {
          setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
          setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }
      val project = factory.newDocumentBuilder().parse(pom).documentElement
      val licenses =
        project.child("licenses")?.children("license").orEmpty().map {
          DeclaredLicense(it.text("name"), it.text("url"))
        }
      val parent =
        project.child("parent")?.let {
          "${it.text("groupId")}:${it.text("artifactId")}:${it.text("version")}"
        }
      return PomLicenses(licenses, parent)
    }

    private fun Element.children(tag: String): List<Element> =
      (0 until childNodes.length)
        .map { childNodes.item(it) }
        .filterIsInstance<Element>()
        .filter { it.tagName == tag }

    private fun Element.child(tag: String): Element? = children(tag).firstOrNull()

    private fun Element.text(tag: String): String = child(tag)?.textContent?.trim().orEmpty()
  }
}
