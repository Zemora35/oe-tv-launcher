package nl.neerdael.backdrop_launcher.ui.cinematic

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.view.View
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.MaterialTheme
import java.io.File
import java.util.Locale
import nl.neerdael.backdrop_launcher.R
import nl.neerdael.backdrop_launcher.ui.loadUiLanguage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InternationalHomeTest {
    @get:Rule val compose = createComposeRule()
    private val base get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun context(tag: String, fontScale: Float = 1f): Context = base.createConfigurationContext(
        Configuration(base.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)); this.fontScale = fontScale })

    @Test fun resourceSelectionFallbackPluralsAndLegacyLabelsWorkAcrossLocales() {
        val homes = mapOf("en" to "Home", "tr" to "Ana Sayfa", "es" to "Inicio", "vi" to "Trang chủ", "ar" to "الرئيسية")
        for ((language, home) in homes) {
            val localized = context(language)
            assertEquals(home, localized.getString(R.string.vortex_nav_home))
            for (count in listOf(0, 1, 2, 3, 11, 100)) {
                val countText = localized.resources.getQuantityString(R.plurals.vortex_app_count, count, count)
                assertTrue(countText.isNotBlank()); assertFalse(countText.contains("%1\$d"))
            }
            assertTrue(loadUiLanguage(localized, language).getValue("Settings").isNotBlank())
        }
        assertEquals("Home", context("ja-JP").getString(R.string.vortex_nav_home))
        assertEquals("Home", context("pl-PL").getString(R.string.vortex_nav_home))
        assertEquals("Inicio", context("es-MX").getString(R.string.vortex_nav_home))
        assertEquals("Arayüz", loadUiLanguage(context("tr-TR"), "tr-TR")["Interface"])
        assertEquals(View.LAYOUT_DIRECTION_RTL, context("ar").resources.configuration.layoutDirection)
    }

    @Test fun localizedScreensAndExpandedPseudoLocaleRemainVisible() {
        var tag by mutableStateOf("en")
        var fontScale by mutableFloatStateOf(1f)
        compose.setContent {
            val localized = remember(tag, fontScale) { context(tag, fontScale) }
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalContext provides localized,
                LocalConfiguration provides localized.resources.configuration,
                LocalResources provides localized.resources,
                LocalLayoutDirection provides if (localized.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL) LayoutDirection.Rtl else LayoutDirection.Ltr,
                LocalDensity provides Density(density, fontScale), LocalInspectionMode provides true) {
                val sample = localizedSamples()
                val apps = if (tag == "ja-JP") sample.apps.mapIndexed { index, app ->
                    if (index == 4) app.copy(packageName = "sample:unicode", label = "📺 日本語中文한국어", artwork = null) else app
                } else sample.apps
                MaterialTheme {
                    CinematicHomeScreen(sample.featured, sample.featured.artwork!!, apps, 4, apps.take(6),
                        localized.getString(R.string.vortex_entertainment), remember { CinemaFocus() },
                        {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
                }
            }
        }
        for (language in listOf("en", "tr", "es", "vi", "ar", "ja-JP", "en-XA", "ar-XB")) {
            compose.runOnIdle { tag = language; fontScale = if (language == "en-XA") 1.15f else 1f }
            val localized = context(language)
            compose.onNodeWithContentDescription(localized.getString(R.string.vortex_nav_home)).assertIsDisplayed()
            compose.onNodeWithContentDescription(localized.getString(R.string.vortex_all_apps)).assertIsDisplayed()
            compose.onNodeWithTag("vortex-dock").assertIsDisplayed()
            if (language == "en-XA") {
                val button = compose.onNodeWithContentDescription(localized.getString(R.string.vortex_content_watch_options)).fetchSemanticsNode().boundsInRoot
                val firstTile = compose.onNodeWithTag("vortex-tile:sample:0", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
                assertTrue("Expanded hero actions must not overlap the app lane", button.bottom < firstTile.top)
            }
            val output = File(base.getExternalFilesDir(null), "vortex-$language.png")
            output.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun rtlDockMirrorsItsCurveAndUsesPhysicalRemoteDirections() {
        var selected by mutableIntStateOf(4)
        val localized = context("ar")
        compose.setContent {
            val focus = remember { FocusRequester() }
            CompositionLocalProvider(LocalContext provides localized, LocalResources provides localized.resources,
                LocalLayoutDirection provides LayoutDirection.Rtl, LocalVortexMotion provides false) {
                MaterialTheme {
                    VortexDock(CinematicSamples.apps, selected, focus, null, true, 0,
                        { selected = VortexGeometry.step(selected, it, CinematicSamples.apps.size) },
                        {}, {}, {}, {}, {}, Modifier.fillMaxSize())
                }
            }
            LaunchedEffect(Unit) { focus.requestFocus() }
        }
        val dock = compose.onNodeWithTag("vortex-dock")
        val centre = compose.onNodeWithTag("vortex-tile:sample:4", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.center.x
        val next = compose.onNodeWithTag("vortex-tile:sample:5", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.center.x
        assertTrue("Logical next app is on the left in RTL", next < centre)
        assertEquals(dock.fetchSemanticsNode().boundsInRoot.center.x, centre, 2f)
        dock.performKeyInput { pressKey(Key.DirectionLeft) }
        compose.runOnIdle { assertEquals(5, selected) }
        dock.assertIsFocused()
        dock.performKeyInput { pressKey(Key.DirectionRight) }
        compose.runOnIdle { assertEquals(4, selected) }
    }
}
