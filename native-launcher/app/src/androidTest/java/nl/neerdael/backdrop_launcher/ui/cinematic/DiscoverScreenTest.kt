package nl.neerdael.backdrop_launcher.ui.cinematic

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.MaterialTheme
import java.io.File
import java.util.Locale
import nl.neerdael.backdrop_launcher.R
import nl.neerdael.backdrop_launcher.core.ResumeItem
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DiscoverScreenTest {
    @get:Rule val compose = createComposeRule()
    private val base get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun item(index: Int) = ResumeItem(if (index == 3) "03 İstanbul Gecesi" else "%02d Programme".format(index),
        "Synopsis", CinematicSamples.watching[index % 3].image, 0, "video://$index", "video.${index % 2}",
        index.toLong(), sourceName = if (index % 2 == 0) "Platform A" else "Platform B", continuation = false)
    private fun card(index: Int) = compose.onNodeWithTag("discover:${contentKey(item(index))}")

    @Test fun remoteNavigatesOffscreenRowsOpensExactContentAndRestoresFocus() {
        val active = mutableStateOf(true)
        var saved by mutableStateOf(emptySet<String>())
        val opened = mutableListOf<ResumeItem>()
        compose.setContent { MaterialTheme {
            CinematicDiscoverScreen((0 until 42).map(::item), saved, "", {}, { opened += it },
                { saved = saved + contentKey(it) }, {}, active = active.value)
        } }
        card(0).assertIsFocused().performKeyInput { pressKey(Key.DirectionRight) }
        for (index in 1..36 step 5) card(index).assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
        card(41).assertIsFocused().assertIsDisplayed().performKeyInput { pressKey(Key.DirectionCenter) }
        card(41).performKeyInput { keyDown(Key.DirectionCenter) }
        compose.mainClock.advanceTimeBy(1000)
        card(41).performKeyInput { keyUp(Key.DirectionCenter) }
        card(41).assertIsSelected()
        compose.runOnIdle { assertEquals(listOf("video://41"), opened.map { it.intentUri }); active.value = false }
        compose.runOnIdle { active.value = true }
        card(41).assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
        card(41).assertIsFocused()
    }

    @Test fun searchPlatformSortAndSavedFiltersUseTheSameGrid() {
        val saved = setOf(contentKey(item(1)), contentKey(item(3)))
        compose.setContent { MaterialTheme { CinematicDiscoverScreen((0 until 42).map(::item), saved, "", {}, {}, {}, {}) } }
        compose.onNodeWithTag("app-grid-search").performTextReplacement("istanbul")
        card(3).assertIsDisplayed(); card(0).assertDoesNotExist()
        compose.onNodeWithTag("app-grid-clear").performClick()
        compose.onNodeWithTag("app-grid-filter").performClick()
        compose.onNodeWithTag("app-grid-choice:2").performClick()
        card(1).assertIsDisplayed(); card(0).assertDoesNotExist()
        compose.onNodeWithTag("app-grid-sort").performClick()
        compose.onNodeWithTag("app-grid-choice:2").performClick()
        card(41).assertIsDisplayed()
        compose.onNodeWithTag("app-grid-category:SAVED").performClick()
        card(3).assertIsDisplayed(); card(1).assertIsDisplayed(); card(5).assertDoesNotExist()
    }

    @Test fun longPlatformListsScrollAndReopenWithTheSelectedSourceFocused() {
        val items = (0..12).map { item(it).copy(packageName = "video.$it", sourceName = "Platform $it") }
        compose.setContent { MaterialTheme { CinematicDiscoverScreen(items, emptySet(), "", {}, {}, {}, {}) } }
        compose.onNodeWithTag("app-grid-filter").performClick()
        compose.onNodeWithTag("grid-choice-list").performScrollToIndex(13)
        compose.onNodeWithTag("app-grid-choice:13").performClick()
        compose.onNodeWithTag("discover:${contentKey(items.last())}").assertIsDisplayed()
        compose.onNodeWithTag("app-grid-filter").performClick()
        compose.onNodeWithTag("app-grid-choice:13").assertIsFocused()
    }

    @Test fun localizedRtlCardsAndEmptyStateRemainRemoteAccessible() {
        var tag by mutableStateOf("tr")
        var empty by mutableStateOf(false)
        var closed = 0
        compose.setContent {
            val localized = remember(tag) { base.createConfigurationContext(Configuration(base.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(tag))
            }) }
            CompositionLocalProvider(LocalContext provides localized, LocalResources provides localized.resources,
                LocalConfiguration provides localized.resources.configuration,
                LocalDensity provides Density(LocalDensity.current.density, 1.15f), LocalInspectionMode provides true,
                LocalLayoutDirection provides if (tag == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr) {
                MaterialTheme { CinematicDiscoverScreen(if (empty) emptyList() else (0..9).map(::item), emptySet(), "",
                    { closed++ }, {}, {}, {}) }
            }
        }
        for (language in listOf("tr", "en", "es", "vi", "ar")) {
            compose.runOnIdle { tag = language }
            compose.onNodeWithTag("discover-screen").assertIsDisplayed()
            card(0).assertIsDisplayed()
        }
        card(0).performKeyInput { pressKey(Key.DirectionLeft) }
        card(1).assertIsFocused()
        File(base.getExternalFilesDir(null), "oe-discover-ar.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.runOnIdle { tag = "tr" }
        File(base.getExternalFilesDir(null), "oe-discover-tr.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.runOnIdle { empty = true }
        compose.onNodeWithTag("app-grid-empty").assertIsDisplayed()
        compose.onNodeWithTag("app-grid-back").assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        compose.runOnIdle { assertEquals(1, closed) }
    }
}
