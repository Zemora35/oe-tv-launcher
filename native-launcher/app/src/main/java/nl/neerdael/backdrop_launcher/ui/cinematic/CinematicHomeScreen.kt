package nl.neerdael.backdrop_launcher.ui.cinematic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.res.stringResource
import nl.neerdael.backdrop_launcher.R
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.zIndex
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import nl.neerdael.backdrop_launcher.core.*
import nl.neerdael.backdrop_launcher.ui.ComposeArtwork

internal class CinemaFocus {
    val nav = FocusRequester()
    val hero = FocusRequester()
    val row = FocusRequester()
    val dock = FocusRequester()
    val quickLaunch = FocusRequester()
    val shortcuts = FocusRequester()
}

/** Pure presentation entry point; callers supply platform/data actions and artwork ownership. */
@Composable
internal fun CinematicHomeScreen(featured: FeaturedContent, backdrop: String, apps: List<AppTile>,
    selectedIndex: Int, quickApps: List<AppTile>, rowLabel: String, focus: CinemaFocus,
    onStep: (Int) -> Unit, onOpen: (AppTile) -> Unit, onHold: (AppTile) -> Unit,
    onRow: (Int) -> Unit, onRows: () -> Unit, onWatch: () -> Unit, onInfo: () -> Unit,
    onTab: (HomeTab) -> Unit, onShortcut: (Shortcut) -> Unit,
    onQuickLaunch: (AppTile) -> Unit, onQuickEdit: () -> Unit, onDockFocus: () -> Unit,
    loader: ComposeArtwork? = null, active: Boolean = true, revision: Int = 0,
    reordering: Boolean = false, restoreShortcut: Shortcut = Shortcut.MY_LIST,
    googleTvAvailable: Boolean = false, onGoogleTv: () -> Unit = {},
    onNextFeatured: (() -> Unit)? = null, onHeroFocus: (Boolean) -> Unit = {}, onQuickFocus: () -> Unit = {}, status: @Composable () -> Unit = {}) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val windowSize = LocalWindowInfo.current.containerSize
    val constrained = rememberConstrainedTv()
    val backdropSize = remember(windowSize, constrained) {
        CinemaPerformance.backdropSize(windowSize.width, windowSize.height, constrained)
    }
    Box(Modifier.fillMaxSize().background(VortexInk)) {
        CinematicArt(featured.artwork ?: backdrop, loader, active, revision, Modifier.fillMaxSize(),
            retain = false, size = backdropSize)
        Box(Modifier.fillMaxSize().drawWithCache {
            val start = Color(0xe6080d1b)
            val middle = Color(0x99080d1b)
            val end = Color(0x08080d1b)
            val horizontal = if (rtl) Brush.horizontalGradient(0f to end, .62f to middle, 1f to start)
                else Brush.horizontalGradient(0f to start, .38f to middle, 1f to end)
            val vertical = Brush.verticalGradient(0f to Color(0x66080d1b), .3f to Color.Transparent, .65f to Color(0x55080d1b), 1f to VortexInk)
            onDrawBehind { drawRect(horizontal); drawRect(vertical) }
        })
        Column(Modifier.fillMaxSize().padding(horizontal = 34.dp, vertical = 18.dp)) {
            TopNavBar(HomeTab.HOME, focus.nav, onTab, { focus.hero.requestFocus() }, status)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                VortexDock(apps, selectedIndex, focus.dock, loader, active, revision, onStep, onOpen, onHold,
                    { focus.row.requestFocus() }, { focus.quickLaunch.requestFocus() }, onDockFocus,
                    Modifier.fillMaxSize().padding(top = 38.dp), reordering)
                // Paint the hero above the dock. Opaque action surfaces occlude icons
                // naturally at their rounded edges, without rectangular cuts or mask layers.
                HeroBanner(featured, focus.hero, onWatch, onInfo, { focus.dock.requestFocus() },
                    { focus.nav.requestFocus() }, Modifier.align(Alignment.TopStart).zIndex(1f)
                        .padding(top = 22.dp).fillMaxWidth(.44f).testTag("hero-banner"),
                    onNext = onNextFeatured, onHeroFocus = onHeroFocus)
                // The row picker occupies otherwise empty space above the right side of the curve.
                CinemaButton(rowLabel, onRows, Modifier.align(Alignment.TopEnd).padding(top = 16.dp)
                    .then(Modifier.cinemaRowInput(focus, onRow, rtl)))
            }
            Row(Modifier.fillMaxWidth().height(140.dp).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                QuickLaunchRow(quickApps, focus.quickLaunch, onQuickLaunch, onQuickEdit,
                    { focus.dock.requestFocus() }, { focus.shortcuts.requestFocus() }, Modifier.weight(1f), loader, active, revision,
                    googleTvAvailable = googleTvAvailable, onGoogleTv = onGoogleTv, onFocus = onQuickFocus)
                Box(Modifier.width(1.dp).height(116.dp).background(Color(0x33717d98)))
                QuickShortcutsPanel(focus.shortcuts, onShortcut, { focus.dock.requestFocus() },
                    { focus.quickLaunch.requestFocus() }, Modifier.width(280.dp), restoreShortcut)
            }
        }
    }
}

@Preview(name = "O.E tv · 1080p", device = "spec:width=1920px,height=1080px,dpi=320", showBackground = true)
@Preview(name = "O.E tv · 720p", device = "spec:width=1280px,height=720px,dpi=213", showBackground = true)
@Preview(name = "O.E tv · Türkçe", locale = "tr", device = "spec:width=1920px,height=1080px,dpi=320")
@Preview(name = "O.E tv · Arabic RTL", locale = "ar", device = "spec:width=1920px,height=1080px,dpi=320")
@Composable
internal fun CinematicHomePreview() {
    MaterialTheme {
        CompositionLocalProvider(LocalVortexMotion provides false) {
            val samples = localizedSamples()
            CinematicHomeScreen(samples.featured, samples.featured.artwork!!,
                samples.apps, 4, samples.apps.take(6), stringResource(R.string.vortex_entertainment), remember { CinemaFocus() },
                {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, status = { Text("21:24", color = Color.White, fontSize = 18.sp) })
        }
    }
}
