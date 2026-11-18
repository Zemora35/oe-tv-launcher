package nl.neerdael.backdrop_launcher.ui.cinematic

import nl.neerdael.backdrop_launcher.core.*
import org.junit.Assert.*
import org.junit.Test

class FeaturedRotationTest {
    private val scene = Scene("Home", bundledBackdropMetadata.keys.first(), emptyList(), emptyList(), false, true, -1, .1f, false, true)
    private fun programme(id: Long, image: String? = "https://example.org/$id.jpg") =
        ResumeItem("Programme $id", "", image, 0, "video://$id", "video.app", id, continuation = false)

    @Test fun selectionAdvancesWrapsAndRecoversFromRemovedContent() {
        val keys = listOf("a", "b", "c")
        assertEquals("b", nextFeaturedKey(keys, "a"))
        assertEquals("a", nextFeaturedKey(keys, "c"))
        assertEquals("a", nextFeaturedKey(keys, "removed"))
        assertEquals("a", nextFeaturedKey(listOf("a"), "a"))
        assertNull(nextFeaturedKey(emptyList(), "a"))
    }

    @Test fun providerChoicesHaveTheirOwnArtworkActionsAndStableKeys() {
        val items = listOf(programme(1), programme(2), programme(1), programme(3, null))
        val choices = featuredChoices(HomeData(scene, "", recommendations = items))
        assertEquals(listOf(contentKey(items[0]), contentKey(items[1])), choices.map { it.key })
        assertEquals("video://2", choices[1].programme?.intentUri)
        assertEquals("https://example.org/2.jpg", choices[1].programme?.image)
        assertTrue(choices.all { it.backdrop == null })
    }

    @Test fun offlineCarouselStartsWithCurrentWallpaperAndIsBounded() {
        val choices = featuredChoices(HomeData(scene, ""))
        assertEquals(scene.backdrop, choices.first().backdrop)
        assertEquals(bundledBackdropMetadata.keys, choices.map { it.backdrop }.toSet())
        assertTrue(choices.all { it.programme == null })
        assertEquals(36, featuredChoices(HomeData(scene, "", recommendations = (1L..100).map { programme(it) })).size)
    }

    @Test fun englishSentencesAreRecognizedWithoutTreatingTurkishWithBrandNamesAsEnglish() {
        assertTrue(isEnglishSynopsis("After the heroes return from their journey, a mage sets out to understand the people she knew."))
        assertTrue(isEnglishSynopsis("He is a man who has lost his way in the world."))
        assertFalse(isEnglishSynopsis("Netflix ve Prime Video için önerilen bir film."))
        assertFalse(isEnglishSynopsis("The Last of Us dizisinde Joel ve Ellie yeni bir yolculuğa çıkar."))
        assertFalse(isEnglishSynopsis("Frieren: Beyond Journey’s End"))
        assertFalse(isEnglishSynopsis(""))
    }
}
