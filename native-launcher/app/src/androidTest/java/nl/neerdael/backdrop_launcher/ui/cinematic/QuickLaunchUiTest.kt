package nl.neerdael.backdrop_launcher.ui.cinematic

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.MaterialTheme
import nl.neerdael.backdrop_launcher.R
import nl.neerdael.backdrop_launcher.core.LauncherApp
import nl.neerdael.backdrop_launcher.data.LegacyPreferences
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class QuickLaunchUiTest {
    @get:Rule val compose = createComposeRule()
    private val base get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val apps = CinematicSamples.apps.take(8).map { LauncherApp(it.packageName, it.label, "1", true, false, null, null) }

    @Test fun narrowRowScrollsToTheLastFavouriteAndRestoresItsAnchorFromEdit() {
        var shortcuts = 0
        compose.setContent { MaterialTheme {
            val focus = remember { FocusRequester() }
            QuickLaunchRow(CinematicSamples.apps.take(6), focus, {}, {}, {}, { shortcuts++ }, Modifier.width(320.dp))
            LaunchedEffect(Unit) { withFrameNanos { }; focus.requestFocus() }
        } }
        for (index in 0..4) {
            compose.onNodeWithTag("quick-launch:sample:$index").assertIsFocused().performKeyInput { pressKey(Key.DirectionRight) }
        }
        val last = compose.onNodeWithTag("quick-launch:sample:5")
        last.assertIsFocused().assertIsDisplayed().performKeyInput { pressKey(Key.DirectionUp) }
        compose.onNodeWithTag("quick-launch-edit").assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
        last.assertIsFocused().assertIsDisplayed().performKeyInput { pressKey(Key.DirectionRight) }
        compose.runOnIdle { assertEquals(1, shortcuts) }
    }

    @Test fun remoteLaunchesTheFocusedAppAndLongSelectEditsWithoutLaunchingAgainInRtl() {
        val opened = mutableListOf<String>()
        var edits = 0
        compose.setContent { MaterialTheme {
            val focus = remember { FocusRequester() }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                QuickLaunchRow(CinematicSamples.apps.take(6), focus, { opened += it.packageName }, { edits++ }, {}, {}, Modifier.width(560.dp))
            }
            LaunchedEffect(Unit) { withFrameNanos { }; focus.requestFocus() }
        } }
        compose.onNodeWithTag("quick-launch:sample:0").assertIsFocused().performKeyInput { pressKey(Key.DirectionLeft) }
        val second = compose.onNodeWithTag("quick-launch:sample:1")
        second.assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        second.performKeyInput { keyDown(Key.DirectionCenter) }
        compose.mainClock.advanceTimeBy(1000)
        second.performKeyInput { keyUp(Key.DirectionCenter) }
        compose.runOnIdle { assertEquals(listOf("sample:1"), opened); assertEquals(1, edits) }
    }

    @Test fun pickerCapsSelectionPreservesOrderAndSavesThroughTheRealPreferenceStore() {
        val prefs = LegacyPreferences(base)
        val before = readQuickLaunch(prefs)
        var editing by mutableStateOf(true)
        var selected by mutableStateOf(apps.take(6).map { it.packageName })
        try {
            compose.setContent { MaterialTheme {
                if (editing) CinematicAppsScreen(buildAppCatalog(apps, emptyList(), emptyMap()), false, "",
                    onClose = { prefs.putStrings(QUICK_LAUNCH_KEY, selected); editing = false },
                    onLaunch = { selected = toggleQuickLaunch(selected, it) }, onHold = {}, onTab = {}, quickSelection = selected)
                else QuickLaunchRow(quickLaunchApps(apps, readQuickLaunch(prefs), "self"), remember { FocusRequester() }, {}, {}, {}, {}, Modifier.width(560.dp))
            } }
            compose.onNodeWithTag("quick-launch-editor").assertIsDisplayed()
            compose.onNodeWithTag("app-grid:sample:6").performClick().assertIsNotSelected()
            compose.runOnIdle { assertEquals(6, selected.size) }
            compose.onNodeWithTag("app-grid:sample:0").performClick().assertIsNotSelected()
            compose.onNodeWithTag("app-grid:sample:6").performClick().assertIsSelected()
            compose.onNodeWithTag("app-grid-back").performClick()
            compose.onNodeWithTag("quick-launch:sample:6").assertIsDisplayed()
            compose.onNodeWithTag("quick-launch:sample:0").assertDoesNotExist()
            compose.runOnIdle {
                assertEquals(listOf("sample:1", "sample:2", "sample:3", "sample:4", "sample:5", "sample:6"), readQuickLaunch(LegacyPreferences(base)))
            }
        } finally {
            if (before == null) prefs.remove(QUICK_LAUNCH_KEY) else prefs.putStrings(QUICK_LAUNCH_KEY, before)
        }
    }

    @Test fun explicitlyEmptyFavouritesSurviveReloadAndKeepEditAndGoogleTvReachable() {
        val prefs = LegacyPreferences(base)
        val before = readQuickLaunch(prefs)
        var edited = 0
        var google = 0
        try {
            prefs.putStrings(QUICK_LAUNCH_KEY, emptyList())
            val reloaded = readQuickLaunch(LegacyPreferences(base))
            assertEquals(emptyList<String>(), reloaded)
            compose.setContent { MaterialTheme {
                val focus = remember { FocusRequester() }
                QuickLaunchRow(quickLaunchApps(apps, reloaded, "self"), focus, {}, { edited++ }, {}, {}, Modifier.width(560.dp),
                    googleTvAvailable = true, onGoogleTv = { google++ })
                LaunchedEffect(Unit) { focus.requestFocus() }
            } }
            compose.onNodeWithTag("quick-launch-edit").assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
            compose.onNodeWithTag("quick-launch-google-tv").performClick()
            compose.onNodeWithText(base.getString(R.string.vortex_quick_launch_empty)).assertIsDisplayed()
            compose.runOnIdle { assertEquals(1, edited); assertEquals(1, google) }
        } finally {
            if (before == null) prefs.remove(QUICK_LAUNCH_KEY) else prefs.putStrings(QUICK_LAUNCH_KEY, before)
        }
    }
}
