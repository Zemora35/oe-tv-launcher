package nl.neerdael.backdrop_launcher.ui.cinematic

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.MaterialTheme
import nl.neerdael.backdrop_launcher.R
import nl.neerdael.backdrop_launcher.core.LauncherApp
import nl.neerdael.backdrop_launcher.core.ResumeItem
import java.io.File
import java.util.Locale
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SearchScreenTest {
    @get:Rule val compose = createComposeRule()
    private val apps = (0..15).map { LauncherApp("sample:$it", if (it == 0) "İzle Plus" else "App $it", "1", true, false, null, null) }
    private val programme = ResumeItem("İzle Gecesi", "Synopsis", null, 0, "video://one", "video.app", 1,
        sourceName = "Example TV", continuation = false)

    @Test fun backLeavesTypingBeforeClosingSearchEvenWithoutImeInsets() {
        var closed = 0
        lateinit var back: OnBackPressedDispatcher
        compose.setContent { MaterialTheme {
            back = checkNotNull(LocalOnBackPressedDispatcherOwner.current).onBackPressedDispatcher
            CinematicSearchScreen(buildAppCatalog(apps, emptyList(), emptyMap()), listOf(programme), emptySet(), "",
                { closed++ }, {}, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("app-grid-search").performTextReplacement("izle")
        // Compose key injection bypasses the Activity's Back dispatcher.
        compose.runOnIdle { back.onBackPressed() }
        compose.onNodeWithTag("search-screen").assertIsDisplayed()
        compose.onNodeWithTag("search-mode:apps").assertIsFocused()
        compose.onNodeWithTag("app-grid:sample:0").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, closed) }
        compose.runOnIdle { back.onBackPressed() }
        compose.runOnIdle { assertEquals(1, closed) }
    }

    @Test fun localizedSearchPreviewSupportsLargeTextAndRtl() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        var tag by mutableStateOf("tr")
        compose.setContent {
            val localized = remember(tag) { base.createConfigurationContext(Configuration(base.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(tag))
            }) }
            CompositionLocalProvider(LocalContext provides localized, LocalResources provides localized.resources,
                LocalConfiguration provides localized.resources.configuration,
                LocalDensity provides Density(LocalDensity.current.density, 1.15f), LocalInspectionMode provides true,
                LocalLayoutDirection provides if (tag == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr) {
                CinematicSearchPreview()
            }
        }
        for (language in listOf("en", "tr", "es", "vi", "ar")) {
            compose.runOnIdle { tag = language }
            compose.onNodeWithTag("search-screen").assertIsDisplayed()
            compose.onNodeWithTag("app-grid-search").assertIsFocused()
            compose.onNodeWithTag("search-mode:apps").assertIsDisplayed()
            compose.onNodeWithTag("search-mode:content").assertIsDisplayed()
            if (language == "tr" || language == "ar") File(base.getExternalFilesDir(null), "oe-search-$language.png").outputStream().use {
                compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    @Test fun oneQuerySearchesAppsAndContentWithSeparateLaunchTargets() {
        val appLaunches = mutableListOf<String>()
        val contentLaunches = mutableListOf<String?>()
        compose.setContent { MaterialTheme {
            CinematicSearchScreen(buildAppCatalog(apps, emptyList(), emptyMap()), listOf(programme), emptySet(), "", {},
                { appLaunches += it }, {}, { contentLaunches += it.intentUri }, {}, {})
        } }
        val field = compose.onNodeWithTag("app-grid-search")
        field.assertIsFocused().performTextReplacement("izle")
        compose.onNodeWithTag("app-grid:sample:0").assertIsDisplayed()
        compose.onNodeWithTag("search-mode:apps").assertIsSelected()
        compose.onNodeWithTag("search-mode:content").performClick().assertIsSelected()
        field.assertTextContains("izle")
        compose.onNodeWithTag("discover:${contentKey(programme)}").assertIsDisplayed().performClick()
        compose.onNodeWithTag("search-mode:apps").performClick()
        field.assertTextContains("izle")
        compose.onNodeWithTag("app-grid:sample:0").performClick()
        compose.runOnIdle {
            assertEquals(listOf("sample:0"), appLaunches)
            assertEquals(listOf("video://one"), contentLaunches)
        }
    }

    @Test fun remoteAppSelectHoldAndReturningFocusWorkFromTheSearchField() {
        val active = mutableStateOf(true)
        var launched = 0
        var held = 0
        compose.setContent { MaterialTheme {
            CinematicSearchScreen(buildAppCatalog(apps, emptyList(), emptyMap()), listOf(programme), emptySet(), "", {},
                { launched++ }, { held++ }, {}, {}, {}, active = active.value)
        } }
        compose.onNodeWithTag("app-grid-search").performTextReplacement("izle")
        compose.onNodeWithTag("app-grid-search").performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag("search-mode:apps").assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
        val card = compose.onNodeWithTag("app-grid:sample:0")
        card.assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        card.performKeyInput { keyDown(Key.DirectionCenter) }
        compose.mainClock.advanceTimeBy(1000)
        card.performKeyInput { keyUp(Key.DirectionCenter) }
        compose.runOnIdle { assertEquals(1, launched); assertEquals(1, held); active.value = false }
        compose.runOnIdle { active.value = true }
        card.assertIsFocused()
    }

    @Test fun rtlEmptyResultsKeepInputModeSwitchAndBackAccessible() {
        var closed = 0
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        compose.setContent { MaterialTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                CinematicSearchScreen(buildAppCatalog(apps, emptyList(), emptyMap()), listOf(programme), emptySet(), "",
                    { closed++ }, {}, {}, {}, {}, {})
            }
        } }
        val field = compose.onNodeWithTag("app-grid-search")
        field.performTextReplacement("no match")
        compose.onNodeWithTag("app-grid-empty").assertIsDisplayed()
        field.assertIsFocused().performKeyInput { pressKey(Key.DirectionUp) }
        compose.onNodeWithContentDescription(base.getString(R.string.vortex_nav_search)).assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }
        field.assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag("search-mode:apps").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionLeft); pressKey(Key.DirectionCenter)
        }
        compose.onNodeWithTag("search-mode:content").assertIsFocused().assertIsSelected()
            .performKeyInput { pressKey(Key.DirectionDown) }
        field.assertIsFocused()
        compose.onNodeWithTag("app-grid-back").performClick()
        compose.runOnIdle { assertEquals(1, closed) }
    }
}
