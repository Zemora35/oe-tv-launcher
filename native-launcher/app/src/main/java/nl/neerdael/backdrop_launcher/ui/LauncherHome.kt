package nl.neerdael.backdrop_launcher.ui

import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import nl.neerdael.backdrop_launcher.core.*
import nl.neerdael.backdrop_launcher.ui.cinematic.CinematicLauncherScreen

@Composable
fun LauncherScreen(home: HomeData, library: LibrarySnapshot, prefs: PreferenceAccess, media: MediaAccess,
    updates: UpdateAccess, actions: UiActions, selection: HomeSelection,
    artwork: ComposeArtwork, active: Boolean, artworkRevision: Int) {
    val scope = rememberCoroutineScope()
    var version by remember { mutableIntStateOf(0) }
    DisposableEffect(prefs) {
        val unsubscribe = prefs.listen { scope.launch { version++ } }
        onDispose { unsubscribe() }
    }
    val cinematic = remember(version) { prefs.bool("native.vortex.enabled", true) }
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    @Suppress("DEPRECATION")
    val locale = if (android.os.Build.VERSION.SDK_INT >= 24) configuration.locales[0] else configuration.locale
    val native = remember(context, configuration) { nativeUiLanguage(context) }
    val language by produceState<Map<String, String>>(native, context, configuration) {
        value = withContext(Dispatchers.IO) { loadUiLanguage(context, locale.toLanguageTag()) }
    }
    if (cinematic) CompositionLocalProvider(LocalUiLanguage provides language) {
        CinematicLauncherScreen(home, library, prefs, media, updates, actions, selection,
            artwork, active, artworkRevision, version)
    }
    else ClassicLauncherScreen(home, library, prefs, media, updates, actions, selection,
        artwork, active, artworkRevision)
}
