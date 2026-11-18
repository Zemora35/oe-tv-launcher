package nl.neerdael.backdrop_launcher.ui

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test

/** Translation contributions fail CI on duplicate/unknown keys or broken format arguments. */
class LocaleCatalogTest {
    private val root = File(requireNotNull(System.getProperty("vortex.resources")))
    private val placeholder = Regex("%(\\d+)\\\$([sd])")
    private fun strings(file: File): Map<String, String> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }
        val nodes = factory.newDocumentBuilder().parse(file).getElementsByTagName("string")
        return buildMap {
            for (index in 0 until nodes.length) {
                val node = nodes.item(index)
                val name = node.attributes.getNamedItem("name").nodeValue
                assertFalse("Duplicate translation key $name in $file", containsKey(name))
                put(name, node.textContent)
            }
        }
    }
    private fun signature(text: String) = placeholder.findAll(text).map { it.value }.sorted().toList()

    @Test fun allHomeTranslationsHaveCompleteKeysAndMatchingPlaceholders() {
        assertEquals(listOf("%1\$d", "%2\$s"), signature("%2\$s / %1\$d"))
        listOf("vortex_strings.xml", "vortex_content_strings.xml", "vortex_grid_strings.xml", "vortex_playback_strings.xml").forEach(::validateCatalog)
    }

    private fun validateCatalog(fileName: String) {
        val base = strings(File(root, "values/$fileName"))
        val primary = base.filterKeys { !it.startsWith("vortex_legacy_") }
        val directories = root.listFiles().orEmpty().filter { it.isDirectory && it.name.startsWith("values-") && File(it, fileName).isFile }
        assertTrue("No translated resource directories found", directories.isNotEmpty())
        for (directory in directories) {
            val language = directory.name.removePrefix("values-")
            val translated = strings(File(directory, fileName))
            assertTrue("Unknown keys in $language", translated.keys.all { it in base })
            for ((key, source) in primary) {
                assertTrue("Missing $key in $language", key in translated)
                assertEquals("Bad formatting for $key in $language", signature(source), signature(translated.getValue(key)))
            }
            for ((key, value) in translated) assertEquals("Bad legacy formatting $key in $language", signature(base.getValue(key)), signature(value))
        }
    }
}
