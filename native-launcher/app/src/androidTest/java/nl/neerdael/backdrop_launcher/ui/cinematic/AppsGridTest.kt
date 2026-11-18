package nl.neerdael.backdrop_launcher.ui.cinematic

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.MaterialTheme
import java.io.File
import java.util.Locale
import java.util.TimeZone
import nl.neerdael.backdrop_launcher.R
import nl.neerdael.backdrop_launcher.core.LauncherApp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppsGridTest {
    @get:Rule val compose = createComposeRule()
    private val base get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val localized get() = base.createConfigurationContext(Configuration(base.resources.configuration).apply { setLocale(Locale.forLanguageTag("tr")) })
    private val apps = (0 until 42).map { index ->
        val sample = CinematicSamples.apps[index % CinematicSamples.apps.size]
        LauncherApp("sample:$index", "%02d".format(index) + " " + sample.label, "1", index % 2 != 0, false,
            if (index < 30) 1_800_000_000_000L - index * 3_600_000L else null, sample.artwork)
    }
    private val entries get() = buildAppCatalog(apps, listOf("sample:31"), emptyMap())
    private fun screen(history: Boolean = false, active: State<Boolean>? = null, rtl: Boolean = false,
        close: () -> Unit = {}, launch: (String) -> Unit = {}, hold: (String) -> Unit = {}) {
        compose.setContent {
            val context = remember { localized }
            CompositionLocalProvider(LocalContext provides context, LocalResources provides context.resources,
                LocalConfiguration provides context.resources.configuration, LocalInspectionMode provides true,
                LocalVortexMotion provides false, LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                MaterialTheme { CinematicAppsScreen(entries, history, CinematicSamples.featured.artwork!!,
                    close, launch, hold, {}, active = active?.value ?: true) }
            }
        }
    }
    private fun tile(index: Int) = compose.onNodeWithTag("app-grid:sample:$index")
    private fun save(name: String) {
        File(base.getExternalFilesDir(null), name).outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun sevenColumnsNavigateWithRemoteAndRestoreFocusedAppAfterReturn() {
        val active = mutableStateOf(true)
        val opened = mutableListOf<String>()
        var held = 0
        screen(active = active, launch = { opened += it }, hold = { held++ })
        tile(0).assertIsFocused()
        tile(0).performKeyInput { pressKey(Key.DirectionRight) }
        tile(1).assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
        tile(8).assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        compose.runOnIdle { assertEquals(listOf("sample:8"), opened); active.value = false }
        compose.runOnIdle { active.value = true }
        tile(8).assertIsFocused().performKeyInput { keyDown(Key.DirectionCenter) }
        compose.mainClock.advanceTimeBy(1000)
        tile(8).performKeyInput { keyUp(Key.DirectionCenter) }
        compose.runOnIdle { assertEquals(1, held) }
        var current = 8
        for (next in listOf(15, 22, 29, 36, 36)) {
            tile(current).performKeyInput { pressKey(Key.DirectionDown) }
            tile(next).assertIsFocused()
            current = next
        }
        save("apps-grid-scroll.png")
    }

    @Test fun searchCategoryAndTypeFiltersAreRealAndEmptySearchDoesNotCrash() {
        screen()
        compose.onNodeWithTag("app-grid-search").performTextInput("netflix")
        tile(0).assertIsDisplayed()
        tile(1).assertDoesNotExist()
        compose.onNodeWithTag("app-grid-filter").performClick()
        compose.onNodeWithContentDescription(localized.getString(R.string.vortex_grid_type_android)).performClick()
        compose.onNodeWithTag("app-grid-empty").assertIsDisplayed()
        compose.onNodeWithTag("app-grid-filter").performClick()
        compose.onNodeWithTag("app-grid-choice:0").performClick()
        compose.onNodeWithTag("app-grid-clear").performClick()
        compose.onNodeWithTag("app-grid-category:STREAMING").performClick()
        tile(0).assertIsDisplayed()
        tile(10).assertDoesNotExist() // Spotify belongs to Media.
        compose.onNodeWithTag("app-grid-search").performTextInput("no-such-app")
        compose.onNodeWithTag("app-grid-empty").assertIsDisplayed()
    }

    @Test fun historyShowsActualVisitDatesAndLabelsUnknownLegacyTime() {
        screen(history = true)
        val expected = formatVisit(apps.first().lastLaunchedAt, visitDateFormatter(Locale.forLanguageTag("tr"), TimeZone.getDefault()))!!
        compose.onNodeWithTag("app-visit:sample:0", useUnmergedTree = true).assertTextEquals(expected)
        tile(0).assertIsFocused()
        save("history-grid-tr.png")
        compose.onNodeWithTag("app-grid").performScrollToKey("sample:31")
        compose.onNodeWithTag("app-visit:sample:31", useUnmergedTree = true).assertTextEquals(localized.getString(R.string.vortex_grid_time_unknown))
        tile(30).assertDoesNotExist() // Never visited and absent from the legacy list.
    }

    @Test fun sortControlsMirrorLayoutForRtlAndBackActionClosesScreen() {
        var closed = 0
        screen(rtl = true, close = { closed++ })
        val first = tile(0).fetchSemanticsNode().boundsInRoot.center.x
        val next = tile(1).fetchSemanticsNode().boundsInRoot.center.x
        assertTrue(next < first)
        tile(0).performKeyInput { pressKey(Key.DirectionLeft) }
        tile(1).assertIsFocused()
        compose.onNodeWithTag("app-grid-sort").performClick()
        compose.onNodeWithContentDescription(localized.getString(R.string.vortex_grid_sort_za)).performClick()
        compose.onNodeWithTag("app-grid-back").performClick()
        compose.runOnIdle { assertEquals(1, closed) }
    }

    @Test fun mockGridPreviewMatchesTheRequestedLayout() {
        screen()
        tile(0).assertIsFocused()
        val first = tile(0).fetchSemanticsNode().boundsInRoot
        val seventh = tile(6).fetchSemanticsNode().boundsInRoot
        val nextRow = tile(7).fetchSemanticsNode().boundsInRoot
        assertEquals(first.center.y, seventh.center.y, 3f)
        assertTrue(nextRow.top > first.bottom - 5)
        save("apps-grid-tr.png")
    }
}
