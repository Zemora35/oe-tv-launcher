package nl.neerdael.backdrop_launcher.ui.cinematic

import android.content.res.Configuration
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.MaterialTheme
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import java.util.Locale
import nl.neerdael.backdrop_launcher.R
import nl.neerdael.backdrop_launcher.core.*
import nl.neerdael.backdrop_launcher.media.NativeMedia
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CinematicContentTest {
    @get:Rule val compose = createComposeRule()
    private val base get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun resources(tag: String) = base.createConfigurationContext(
        Configuration(base.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) }).resources
    private fun home(image: String, title: String? = null, description: String? = null) = HomeData(
        Scene("Home", image, emptyList(), emptyList(), false, true, -1, .1f, false, true), "", title, description)

    @Test fun featuredProgrammeShowsItsRealPlatformAndBothActionsWork() {
        val programme = ResumeItem("An episode", "Provider synopsis", "content://provider/still", 0,
            "video://episode", "video.app", sourceName = "Example TV", continuation = false)
        var watched = 0
        var informed = 0
        compose.setContent { MaterialTheme {
            HeroBanner(featuredContent(programme, base.resources), remember { FocusRequester() },
                { watched++ }, { informed++ }, {}, {}, Modifier.width(420.dp))
        } }
        compose.onNodeWithTag("hero-platform").assertTextEquals("Example TV")
        compose.onNodeWithContentDescription(base.getString(R.string.vortex_watch_now)).performClick()
        compose.onNodeWithContentDescription(base.getString(R.string.vortex_more_info)).performClick()
        compose.runOnIdle { assertEquals(1, watched); assertEquals(1, informed) }
    }

    @Test fun bundledArtworkOffersViewingOptionsWithoutClaimingASubscriptionProvider() {
        var calls = 0
        compose.setContent { MaterialTheme {
            HeroBanner(resolveFeaturedContent(home("backdrops/the-matrix-553be0c3bdeeb.jpg"), null, base.resources),
                remember { FocusRequester() }, { calls++ }, {}, {}, {}, Modifier.width(420.dp))
        } }
        compose.onNodeWithTag("hero-platform").assertTextEquals(base.getString(R.string.vortex_content_platform_unknown))
        compose.onNodeWithContentDescription(base.getString(R.string.vortex_content_watch_options)).performClick()
        compose.runOnIdle { assertEquals(1, calls) }
    }

    @Test fun youtubeShortcutNeverCreatesFictitiousResumeCards() {
        var opened = 0
        var google = 0
        compose.setContent { MaterialTheme {
            ContinueWatchingRow(emptyList(), null, true, 0, remember { FocusRequester() }, {}, {}, {}, {},
                Modifier.width(560.dp), youtubeAvailable = true, onYouTube = { opened++ },
                googleTvAvailable = true, onGoogleTv = { google++ })
        } }
        compose.onNodeWithText(base.getString(R.string.vortex_content_youtube_hint)).assertIsDisplayed()
        compose.onNodeWithTag("watching-youtube").performClick()
        compose.onNodeWithTag("watching-google-tv").performClick()
        compose.runOnIdle { assertEquals(1, opened); assertEquals(1, google) }
    }

    @Test fun everyBundledPictureHasPairedLocalizedMetadataWithoutNetwork() {
        assertEquals(NativeMedia.BUNDLED_BACKDROPS.toSet(), bundledBackdropMetadata.keys)
        for (tag in listOf("en", "tr", "es", "vi", "ar")) {
            val localized = resources(tag)
            for (image in NativeMedia.BUNDLED_BACKDROPS) {
                val featured = resolveFeaturedContent(home(image), null, localized)
                assertEquals(image, featured.artwork)
                assertTrue(featured.title.isNotBlank())
                assertTrue(featured.description.isNotBlank())
                assertNotEquals(localized.getString(R.string.vortex_home_title), featured.title)
            }
            assertFalse(localized.getQuantityString(R.plurals.vortex_resume_minutes_left, 45, 45).contains("%1\$d"))
        }
        val pulp = resolveFeaturedContent(home("backdrops/pulp-fiction-526d50098865c.jpg"), null, resources("tr"))
        assertEquals("Ucuz Roman", pulp.title)
        assertTrue(pulp.description.contains("Los Angeles"))
    }

    @Test fun aMissingProgrammeImageNeverLabelsAnUnrelatedMovieBackground() {
        val wallpaper = home("backdrops/pulp-fiction-526d50098865c.jpg")
        val episode = ResumeItem("Another series", "Episode synopsis", null, 40, "video://resume", "video.app")
        val fallback = resolveFeaturedContent(wallpaper, episode, resources("tr"))
        assertEquals("Ucuz Roman", fallback.title)
        assertNull(fallback.playable)
        val withArt = episode.copy(image = "content://video.app/episode.jpg")
        val featured = resolveFeaturedContent(wallpaper, withArt, resources("tr"))
        assertEquals(withArt.title, featured.title)
        assertEquals(withArt.description, featured.description)
        assertEquals(withArt.image, featured.artwork)
        assertEquals(withArt, featured.playable)
        val remote = resolveFeaturedContent(home("cache:remote.jpg", "Remote film", "Remote plot"), null, resources("tr"))
        assertEquals("Remote film", remote.title)
        assertEquals("Remote plot", remote.description)
    }

    @Test fun rotatingArtworkChangesTheVisibleNameAndSynopsisTogether() {
        var image by mutableStateOf("backdrops/pulp-fiction-526d50098865c.jpg")
        compose.setContent {
            val featured = resolveFeaturedContent(home(image), null, resources("tr"))
            MaterialTheme {
                CinematicHomeScreen(featured, image, CinematicSamples.apps, 4, emptyList(), "TV Apps",
                    remember { CinemaFocus() }, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
            }
        }
        val pulp = resolveFeaturedContent(home(image), null, resources("tr"))
        compose.onNodeWithText(pulp.title).assertIsDisplayed()
        compose.onNodeWithText(pulp.description).assertIsDisplayed()
        compose.runOnIdle { image = "backdrops/the-lord-of-the-rings-the-return-of-the-king-51121045c2707.jpg" }
        val next = resolveFeaturedContent(home(image), null, resources("tr"))
        compose.onNodeWithText(next.title).assertIsDisplayed()
        compose.onNodeWithText(next.description).assertIsDisplayed() // Compact mode must keep the plot.
        compose.onNodeWithText(pulp.title).assertDoesNotExist()
    }

    @Test fun emptyQuickLaunchOpensItsEditorWithoutRequestingPlaybackPermission() {
        var calls = 0
        compose.setContent {
            MaterialTheme {
                CinematicHomeScreen(CinematicSamples.featured, "", emptyList(), 0, emptyList(), "TV Apps",
                    remember { CinemaFocus() }, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, { calls++ }, {})
            }
        }
        compose.onNodeWithTag("quick-launch-edit").performClick()
        compose.onNodeWithText(base.getString(R.string.vortex_quick_launch_empty)).assertIsDisplayed()
        compose.onNodeWithText(base.getString(R.string.vortex_continue_watching)).assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, calls) }
    }
}
