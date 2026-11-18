package nl.neerdael.backdrop_launcher.ui.cinematic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.tv.material3.MaterialTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File
import nl.neerdael.backdrop_launcher.R

@RunWith(AndroidJUnit4::class)
class VortexRemoteTest {
    @get:Rule val compose = createComposeRule()

    @Test fun tilesNeverPaintOverHeroControlsInEitherLayoutDirection() {
        var showDock by mutableStateOf(false)
        var direction by mutableStateOf(LayoutDirection.Ltr)
        compose.setContent {
            CompositionLocalProvider(LocalVortexMotion provides false, LocalLayoutDirection provides direction) {
                MaterialTheme {
                    CinematicHomeScreen(CinematicSamples.featured.copy(description =
                        "A stunning journey beyond our world, where humanity faces its greatest discovery."),
                        "", if (showDock) CinematicSamples.apps else emptyList(),
                        4, emptyList(), "Entertainment", remember { CinemaFocus() },
                        {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
                }
            }
        }
        for (layout in listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)) {
            compose.runOnIdle { showDock = false; direction = layout }
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val buttons = listOf(R.string.vortex_content_watch_options, R.string.vortex_more_info).map {
                compose.onNodeWithContentDescription(context.getString(it)).fetchSemanticsNode().boundsInRoot
            }
            val before = compose.onRoot().captureToImage().asAndroidBitmap()
            compose.runOnIdle { showDock = true }
            val tile = compose.onNodeWithTag("vortex-tile:sample:0", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertTrue("Fixture must reproduce the Netflix/button overlap", buttons.any { it.overlaps(tile) })
            val selected = compose.onNodeWithTag("vortex-tile:sample:4", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertTrue("Hero controls must leave the centred selected tile intact", buttons.none { it.overlaps(selected) })
            // A full-height central strip stays inside each rounded surface and still
            // includes the lower edge where Netflix used to paint over the button.
            val radius = 14 * context.resources.displayMetrics.density
            val regions = buttons.map { Rect(it.left + radius, it.top + 1, it.right - radius, it.bottom - 1) }
            val after = compose.onRoot().captureToImage().asAndroidBitmap()
            for (region in regions) {
                val left = region.left.toInt().coerceAtLeast(0)
                val top = region.top.toInt().coerceAtLeast(0)
                val width = (region.right.toInt().coerceAtMost(before.width) - left).coerceAtLeast(1)
                val height = (region.bottom.toInt().coerceAtMost(before.height) - top).coerceAtLeast(1)
                val expected = IntArray(width * height)
                val actual = IntArray(width * height)
                before.getPixels(expected, 0, width, left, top, width, height)
                after.getPixels(actual, 0, width, left, top, width, height)
                assertTrue("Dock artwork must remain behind rounded hero controls in $layout", expected.contentEquals(actual))
            }
        }
    }

    @Test fun remoteKeepsAnchorAndActivatesTheSelectedAppDuringRapidReversals() {
        var index by mutableIntStateOf(4)
        val opened = mutableListOf<String>()
        var held = 0
        compose.setContent {
            val focus = remember { FocusRequester() }
            MaterialTheme {
                Box(Modifier.fillMaxSize()) {
                    VortexDock(CinematicSamples.apps, index, focus, null, true, 0,
                        { index = VortexGeometry.step(index, it, CinematicSamples.apps.size) },
                        { opened += it.key }, { held++ }, {}, {}, {}, Modifier.fillMaxSize())
                }
            }
            LaunchedEffect(Unit) { focus.requestFocus() }
        }
        val dock = compose.onNodeWithTag("vortex-dock")
        dock.assertIsFocused()
        dock.performKeyInput {
            pressKey(Key.DirectionRight); pressKey(Key.DirectionRight); pressKey(Key.DirectionLeft)
            pressKey(Key.DirectionCenter)
        }
        compose.runOnIdle { assertEquals(5, index); assertEquals(listOf(CinematicSamples.apps[5].key), opened) }
        dock.assertIsFocused()
        dock.performKeyInput { keyDown(Key.DirectionCenter) }
        compose.mainClock.advanceTimeBy(1000)
        dock.performKeyInput { keyUp(Key.DirectionCenter) }
        compose.runOnIdle { assertEquals(1, held); assertEquals(1, opened.size) }
        dock.assertIsFocused()
    }

    @Test fun directionalInputExitsDockAndReturnsFromQuickLaunch() {
        compose.setContent {
            val focus = remember { CinemaFocus() }
            MaterialTheme {
                CinematicHomeScreen(CinematicSamples.featured, "", CinematicSamples.apps, 4, CinematicSamples.apps.take(6),
                    "Entertainment", focus, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
            }
            LaunchedEffect(Unit) { focus.dock.requestFocus() }
        }
        val dock = compose.onNodeWithTag("vortex-dock")
        dock.performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag("quick-launch:sample:0").assertIsFocused()
        compose.onNodeWithTag("quick-launch:sample:0").performKeyInput { pressKey(Key.DirectionUp) }
        compose.onNodeWithTag("quick-launch-edit").assertIsFocused().performKeyInput { pressKey(Key.DirectionUp) }
        dock.assertIsFocused()
        dock.performKeyInput { pressKey(Key.DirectionUp) }
        compose.onNodeWithContentDescription("Entertainment").assertIsFocused()
    }

    @Test fun capturePreviewWithBundledArtAndRecognisableIcons() {
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) { CinematicHomePreview() }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onAllNodesWithText(context.getString(R.string.vortex_sample_title)).onFirst().assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.vortex_quick_launch)).assertIsDisplayed()
        compose.onNodeWithContentDescription(context.getString(R.string.vortex_nav_home)).assertIsDisplayed()
        compose.onNodeWithContentDescription(context.getString(R.string.vortex_all_apps)).assertIsDisplayed()
        val target = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "vortex-preview.png")
        target.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun selectedAppIsLargerAndReturnsToTheExactCentreAfterEveryStep() {
        var index by mutableIntStateOf(4)
        compose.setContent {
            val focus = remember { FocusRequester() }
            MaterialTheme {
                VortexDock(CinematicSamples.apps, index, focus, null, true, 0,
                    { index = VortexGeometry.step(index, it, CinematicSamples.apps.size) },
                    {}, {}, {}, {}, {}, Modifier.fillMaxSize())
            }
            LaunchedEffect(Unit) { focus.requestFocus() }
        }
        val dock = compose.onNodeWithTag("vortex-dock")
        fun assertCentre() {
            val bounds = dock.fetchSemanticsNode().boundsInRoot
            val selected = compose.onNodeWithTag("vortex-tile:${CinematicSamples.apps[index].key}", useUnmergedTree = true)
                .fetchSemanticsNode().boundsInRoot
            val neighbour = compose.onNodeWithTag("vortex-tile:${CinematicSamples.apps[index + 1].key}", useUnmergedTree = true)
                .fetchSemanticsNode().boundsInRoot
            assertEquals(bounds.center.x, selected.center.x, 2f)
            assertTrue("The selected app should be visibly larger", selected.width > neighbour.width * 1.4f)
        }
        assertCentre()
        dock.performKeyInput { pressKey(Key.DirectionRight) }
        assertCentre()
        dock.performKeyInput { pressKey(Key.DirectionLeft) }
        assertCentre()
    }
}
