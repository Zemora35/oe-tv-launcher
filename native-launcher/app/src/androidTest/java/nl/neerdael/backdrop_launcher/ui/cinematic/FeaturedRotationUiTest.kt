package nl.neerdael.backdrop_launcher.ui.cinematic

import android.content.res.Configuration
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.MaterialTheme
import java.util.Locale
import nl.neerdael.backdrop_launcher.R
import nl.neerdael.backdrop_launcher.core.ResumeItem
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class FeaturedRotationUiTest {
    @get:Rule val compose = createComposeRule()
    private val base get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun resources(tag: String) = base.createConfigurationContext(
        Configuration(base.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) }).resources

    @Test fun automaticRotationSwapsAllMetadataAndPlayTargetTogetherAndPausesOffHome() {
        compose.mainClock.autoAdvance = false
        var active by mutableStateOf(true)
        var selected: FeaturedContent? = null
        var played: String? = null
        val first = ResumeItem("First", "First plot", "first.jpg", 0, "video://first", "first.app", sourceName = "First TV", continuation = false)
        val second = first.copy(title = "Second", description = "Second plot", image = "second.jpg", intentUri = "video://second", packageName = "second.app", sourceName = "Second TV")
        val items = listOf(first, second)
        compose.setContent { MaterialTheme {
            val rotation = rememberFeaturedRotation(items.map(::contentKey), active)
            val item = items.first { contentKey(it) == rotation.key }
            val featured = featuredContent(item, base.resources)
            SideEffect { selected = featured }
            HeroBanner(featured, remember { FocusRequester() }, { played = featured.playable?.intentUri }, {}, {}, {}, Modifier.width(420.dp))
        } }
        compose.mainClock.advanceTimeBy(29_000)
        compose.onNodeWithText("First").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(1_500)
        compose.onNodeWithText("Second").assertIsDisplayed()
        compose.onNodeWithText("Second plot").assertIsDisplayed()
        compose.onNodeWithTag("hero-platform").assertTextEquals("Second TV")
        compose.runOnIdle { assertEquals("second.jpg", selected?.artwork); active = false }
        compose.mainClock.advanceTimeBy(90_000)
        compose.onNodeWithText("Second").assertIsDisplayed()
        compose.onNodeWithContentDescription(base.getString(R.string.vortex_watch_now)).performClick()
        compose.runOnIdle { assertEquals("video://second", played); active = true }
        compose.mainClock.advanceTimeBy(29_000)
        compose.onNodeWithText("Second").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(1_500)
        compose.onNodeWithText("First").assertIsDisplayed()
    }

    @Test fun remoteHeroFocusPausesTimerAndNextStillWorks() {
        compose.mainClock.autoAdvance = false
        val focus = FocusRequester()
        compose.setContent { MaterialTheme {
            var focused by remember { mutableStateOf(false) }
            val rotation = rememberFeaturedRotation(listOf("First", "Second"), !focused)
            HeroBanner(FeaturedContent(rotation.key!!, "Plot", "${rotation.key}.jpg"), focus,
                {}, {}, {}, {}, Modifier.width(420.dp), onNext = rotation.next, onHeroFocus = { focused = it })
        } }
        compose.runOnIdle { focus.requestFocus() }
        compose.mainClock.advanceTimeBy(95_000)
        compose.onNodeWithText("First").assertIsDisplayed()
        compose.onNodeWithContentDescription(base.getString(R.string.vortex_content_watch_options)).performKeyInput { pressKey(Key.DirectionRight); pressKey(Key.DirectionRight) }
        compose.onNodeWithTag("hero-next").assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Second").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(60_000)
        compose.onNodeWithText("Second").assertIsDisplayed()
    }

    @Test fun turkishDescriptionsUseMatchingSummaryOrExplicitLocalizedFallback() {
        val english = "After the heroes return from their journey, a mage sets out to understand the people she knew."
        val tr = resources("tr")
        val first = localizedFeaturedSynopsis("Frieren: Beyond Journey’s End", english, tr)
        assertTrue(first.contains("yolculuğa"))
        assertFalse(first.contains("After the"))
        val native = "İstanbul’da bir ailenin yıllar süren hikâyesi."
        assertEquals(native, localizedFeaturedSynopsis("Another show", native, tr))
        assertEquals(tr.getString(R.string.vortex_synopsis_unavailable), localizedFeaturedSynopsis("Unknown", english, tr))
        assertEquals(english, localizedFeaturedSynopsis("Frieren", english, resources("en")))
        for (locale in listOf("tr", "es", "vi", "ar")) {
            assertFalse(localizedFeaturedSynopsis("Unknown", english, resources(locale)).contains("After the"))
        }
    }
}
