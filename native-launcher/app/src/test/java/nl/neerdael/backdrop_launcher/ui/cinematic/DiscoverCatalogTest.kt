package nl.neerdael.backdrop_launcher.ui.cinematic

import java.util.Locale
import nl.neerdael.backdrop_launcher.core.ResumeItem
import org.junit.Assert.*
import org.junit.Test

class DiscoverCatalogTest {
    private fun item(id: Long, title: String, source: String = "a") = ResumeItem(title, "", "image.jpg", 0,
        "video://$id", source, id, sourceName = "Same label", continuation = false)

    @Test fun stableIdentitiesKeepDifferentProgrammesAndRemoveOnlyRepeatedRecords() {
        val first = item(1, "Same title")
        val second = item(2, "Same title")
        val ordered = orderDiscover(listOf(first, second, first, item(3, " ")), DiscoverSort.RECOMMENDED, Locale.ENGLISH)
        assertEquals(listOf(first, second), ordered)
        assertEquals("video://2", ordered[1].intentUri)
    }
    @Test fun platformIdentityIsIndependentOfItsDisplayNameAndSearchSupportsTurkish() {
        val first = item(1, "Çağrı: İstanbul", "a")
        val second = item(2, "Other", "b")
        val all = listOf(first, second)
        assertEquals(2, discoverSources(all).size)
        assertEquals(listOf(second), filterDiscover(all, "", "b", DiscoverKind.ALL, emptySet()))
        assertEquals(listOf(first), filterDiscover(all, "cagri", null, DiscoverKind.ALL, emptySet()))
        assertEquals(listOf(first), filterDiscover(all, "istanbul", null, DiscoverKind.ALL, emptySet()))
        assertTrue(filterDiscover(all, "missing", null, DiscoverKind.ALL, emptySet()).isEmpty())
    }
    @Test fun savedAndRecommendedFiltersPreserveRealProgressAndLegacySavedKeys() {
        val preview = item(7, "Preview")
        val watching = item(8, "Watching").copy(continuation = true, progress = 42)
        val all = listOf(preview, watching)
        assertEquals(listOf(preview), filterDiscover(all, "", null, DiscoverKind.RECOMMENDED, emptySet()))
        assertEquals(listOf(preview), filterDiscover(all, "", null, DiscoverKind.SAVED, setOf("id:7")))
        assertEquals(42, filterDiscover(all, "", null, DiscoverKind.ALL, emptySet())[1].progress)
        assertTrue(filterDiscover(all, "", null, DiscoverKind.SAVED, emptySet()).isEmpty())
    }
    @Test fun alphabeticalSortDoesNotChangeTheDefaultProviderSequence() {
        val all = listOf(item(1, "Zulu"), item(2, "Alpha"), item(3, "Beta"))
        assertEquals(listOf("Alpha", "Beta", "Zulu"), orderDiscover(all, DiscoverSort.NAME_ASC, Locale.ENGLISH).map { it.title })
        assertEquals(listOf("Zulu", "Beta", "Alpha"), orderDiscover(all, DiscoverSort.NAME_DESC, Locale.ENGLISH).map { it.title })
        assertEquals(all, orderDiscover(all, DiscoverSort.RECOMMENDED, Locale.ENGLISH))
    }
}
